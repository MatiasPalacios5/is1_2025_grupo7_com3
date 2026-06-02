# Manejo de errores en vistas Mustache

## ¿Cómo funciona Mustache para mostrar errores?

Mustache es un motor de plantillas **sin lógica** (logic-less), lo que significa que no soporta sentencias `if`, `else`, o bucles complejos como otros lenguajes de plantillas (por ejemplo, Thymeleaf o JSP). 

En su lugar, Mustache utiliza **secciones condicionales** mediante las etiquetas `{{#variable}}` y `{{^variable}}`:

- `{{#variable}} ... {{/variable}}`: El contenido dentro de esta sección **solo se renderiza** si la variable existe en el modelo y su valor no es falso, nulo o una lista vacía. Funciona como un "if".
- `{{^variable}} ... {{/variable}}`: Se conoce como sección invertida. El contenido se renderiza **solo si la variable no existe**, es nula o falsa. Funciona como un "else".

Al inyectar desde Spark variables como `errorMessage` o `errorDni` en el mapa del modelo, Mustache puede condicionalmente mostrar u ocultar componentes de la interfaz de usuario basándose en si esos errores ocurrieron o no.

## Patrón actual del proyecto

### Desde el controlador (App.java)
El proyecto utiliza redirecciones con parámetros de consulta (`query params`) o pasa directamente las variables en el `Map<String, Object>` modelo cuando ocurre un error de validación o lógica.

Ejemplo en una ruta GET (leyendo parámetros URL):
```java
String errorDni = req.queryParams("errorDni");
if (errorDni != null && !errorDni.isEmpty()) {
    model.put("errorDni", errorDni);
}
```

Ejemplo en una ruta POST (antes del render o en redirección):
```java
if (existingPerson != null) {
    res.status(400);
    res.redirect("/profesor/create?errorDni="
            + URLEncoder.encode("El DNI ya está registrado en el sistema.", StandardCharsets.UTF_8));
    return "";
}
```

### Desde la vista (Mustache)
Las vistas utilizan las secciones condicionales de Mustache para evaluar si la variable del error está definida.

Para inyectar clases de CSS (como en Tailwind) dinámicamente si hay un error:
```html
class="w-full border {{#errorDni}}border-red-500{{/errorDni}}{{^errorDni}}border-gray-300{{/errorDni}}"
```
Y para mostrar el mensaje específico debajo del campo correspondiente:
```html
{{#errorDni}}
<p class="text-red-500 text-sm mt-1">{{errorDni}}</p>
{{/errorDni}}
```

## Tipos de errores identificados

### Error general
Se debe utilizar la variable `errorMessage` para errores que afectan la globalidad de la operación o que no están atados a un campo en particular. 
**Ejemplos**:
- "Usuario o contraseña incorrectos."
- "Todos los campos son obligatorios."
- "Error interno al registrar el estudiante."

### Error por campo específico
Se utilizan variables con el prefijo `error` más el nombre del campo, como `errorDni`, `errorEmail`, `errorLegajo`, para validaciones de campos individuales, usualmente restricciones de unicidad o tipo de dato.
**Ejemplos**:
- `errorDni`: "El DNI ya está registrado en el sistema."
- `errorEmail`: "El email introducido ya existe."

## Buenas prácticas para el equipo

Al implementar nuevas vistas o formularios, el equipo debe adherirse a las siguientes reglas:

1. **Mantener la consistencia del Error General**: Todo formulario debe contar con un bloque para alertas generales utilizando la variable `errorMessage` (y `successMessage` para éxitos) en la parte superior del formulario, antes de cualquier `input`.
2. **Utilizar Errores por Campo cuando sea posible**: Para errores de validación de campos únicos (como email, DNI, legajo) o errores de formato, usar variables de error específicas (ej. `errorUsername`).
3. **Pintar el borde del campo**: Cuando exista un error por campo, el borde del `<input>` correspondiente debe cambiar a rojo (`border-red-500`). En caso contrario, debe mantenerse gris (`border-gray-300`).
4. **Mensaje de ayuda en rojo**: Mostrar el texto del error debajo del `<input>` utilizando el color rojo para alertar al usuario (usando `<p class="text-red-500 text-sm mt-1">`).
5. **No saturar de lógica**: Mantener siempre la lógica de validación en el controlador (`App.java`). Mustache solo debe dedicarse a presentar la cadena de texto de error ya validada.

## Ejemplos concretos

### Ejemplo: validar campo único
```java
// App.java
Teacher existingTeacher = Teacher.findFirst("email = ?", email);
if (existingTeacher != null) {
    res.status(400);
    res.redirect("/profesor/create?errorEmail="
            + URLEncoder.encode("El email introducido ya existe.", StandardCharsets.UTF_8));
    return "";
}

// Vista Mustache
<input type="email" name="email"
       class="border {{#errorEmail}}border-red-500{{/errorEmail}}{{^errorEmail}}border-gray-300{{/errorEmail}} rounded-md">
{{#errorEmail}}
<p class="text-red-500 text-sm mt-1">{{errorEmail}}</p>
{{/errorEmail}}
```

### Ejemplo: validar campo obligatorio vacío
```java
// App.java
if (nombre == null || nombre.isEmpty()) {
    res.status(400);
    res.redirect("/estudiante/create?error=" 
            + URLEncoder.encode("El campo nombre es obligatorio.", StandardCharsets.UTF_8));
    return "";
}

// Vista Mustache
{{#errorMessage}}
<div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-4" role="alert">
    <span class="block sm:inline">{{errorMessage}}</span>
</div>
{{/errorMessage}}
```

---

## Snippet de referencia

Usa el siguiente código listo para copiar y pegar en tus nuevas implementaciones:

### Snippet para el controlador (App.java):
```java
// Error general
String errorMessage = req.queryParams("error");
if (errorMessage != null && !errorMessage.isEmpty()) {
    model.put("errorMessage", errorMessage);
}

// Error por campo específico
String errorCampo = req.queryParams("errorCampo");
if (errorCampo != null && !errorCampo.isEmpty()) {
    model.put("errorCampo", errorCampo);
}
```

### Snippet para la vista (Mustache):
```html
<!-- Error general -->
{{#errorMessage}}
<div class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-4" role="alert">
    <span class="block sm:inline">{{errorMessage}}</span>
</div>
{{/errorMessage}}

<!-- Error por campo específico -->
<input type="text" name="campo"
       class="w-full px-4 py-2 border {{#errorCampo}}border-red-500{{/errorCampo}}{{^errorCampo}}border-gray-300{{/errorCampo}} rounded-md">
{{#errorCampo}}
<p class="text-red-500 text-sm mt-1">{{errorCampo}}</p>
{{/errorCampo}}
```
