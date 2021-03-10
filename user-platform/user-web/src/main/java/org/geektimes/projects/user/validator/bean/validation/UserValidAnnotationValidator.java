package org.geektimes.projects.user.validator.bean.validation;

import org.geektimes.projects.user.domain.User;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class UserValidAnnotationValidator implements ConstraintValidator<UserValid, User> {

    private int passwordNumMin;
    private int passwordNumMax;
    private int phoneNumberNum;

    private Pattern pattern = Pattern.compile("[0-9]+");
    private Pattern blankPattern = Pattern.compile(".*[ \t\n\r]+.*");

    @Override
    public void initialize(UserValid annotation) {
        passwordNumMin = annotation.passwordNumMin();
        passwordNumMax = annotation.passwordNumMax();
        phoneNumberNum = annotation.phoneNumberNum();
    }

    @Override
    public boolean isValid(User value, ConstraintValidatorContext context) {

        // 获取模板信息
        //String tmp = context.getDefaultConstraintMessageTemplate();
        //System.out.println("tmpxxxxxxxx:" + tmp);

        context.disableDefaultConstraintViolation();

        boolean valOk = true;

        String password = value.getPassword();
        if (password == null || password.isEmpty()) {
            context.buildConstraintViolationWithTemplate("密码不能为空").addConstraintViolation();
            valOk = false;
        }
        int i = password.codePointCount(0, password.length());
        if (i > passwordNumMax || i < passwordNumMin) {
            context.buildConstraintViolationWithTemplate("密码位数错误").addConstraintViolation();
            valOk = false;
        }


        String phone = value.getPhoneNumber();
        if (phone == null || phone.isEmpty()) {
            context.buildConstraintViolationWithTemplate("手机号不能为空").addConstraintViolation();
            valOk = false;
        }
        i = phone.codePointCount(0, phone.length());
        if (i != phoneNumberNum) {
            context.buildConstraintViolationWithTemplate("手机号位数错误").addConstraintViolation();
            valOk = false;
        }

        if (!pattern.matcher(phone).matches()) {
            context.buildConstraintViolationWithTemplate("手机号格式错误").addConstraintViolation();
            valOk = false;
        }

        if (blankPattern.matcher(value.getName()).matches()) {
            context.buildConstraintViolationWithTemplate("名字不能包含空格").addConstraintViolation();
            valOk = false;
        }

        if (!value.getPassword().equals(value.getPasswordConfirm())) {
            context.buildConstraintViolationWithTemplate("密码和确认密码不一致").addConstraintViolation();
            valOk = false;
        }

        return valOk;
    }
}
