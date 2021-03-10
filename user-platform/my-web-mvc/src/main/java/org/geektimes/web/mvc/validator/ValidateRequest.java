package org.geektimes.web.mvc.validator;

import org.geektimes.web.mvc.controller.Controller;

import javax.validation.Validator;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidateRequest {

    Class<?> targetClass();
}
