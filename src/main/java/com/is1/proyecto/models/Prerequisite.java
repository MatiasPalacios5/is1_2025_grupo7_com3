package com.is1.proyecto.models;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.BelongsTo;
import org.javalite.activejdbc.annotations.BelongsToParents;
import org.javalite.activejdbc.annotations.Table;

@Table("prerequisites")
@BelongsToParents({
    @BelongsTo(foreignKeyName="id_subject", parent=Subject.class),
    @BelongsTo(foreignKeyName="id_prerequisite", parent=Subject.class)
})
public class Prerequisite extends Model {
}
