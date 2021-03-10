package org.geektimes.projects.user.validator.bean.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UserValidAnnotationValidator.class)
public @interface UserValid {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int passwordNumMin() default 6;

    int passwordNumMax() default 32;

    int phoneNumberNum() default 11;
}
