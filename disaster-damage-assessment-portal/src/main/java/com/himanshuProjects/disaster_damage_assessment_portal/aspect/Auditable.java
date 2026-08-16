package com.himanshuProjects.disaster_damage_assessment_portal.aspect;

import com.himanshuProjects.disaster_damage_assessment_portal.enums.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    AuditAction action();

    String entityName();

    String description() default "";
}
