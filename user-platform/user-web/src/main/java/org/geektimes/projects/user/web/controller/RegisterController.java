package org.geektimes.projects.user.web.controller;

import org.geektimes.projects.user.domain.User;
import org.geektimes.projects.user.service.UserService;
import org.geektimes.web.mvc.controller.PageController;
import org.geektimes.web.mvc.validator.ValidateRequest;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.validation.*;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

/**
 * 输出 “Hello,World” Controller
 */
@Path("/register")
public class RegisterController implements PageController {

    @Resource(name = "bean/UserService")
    UserService userService;

    @GET
    @ValidateRequest(targetClass = User.class)
    public String execute(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        try {

            User user = (User) request.getAttribute("requestObj");

            // userService
            boolean ok = userService.register(user);
            if (!ok) {
                request.setAttribute("errStr", "不好意思啊，出错了。");
                return "err.jsp";
            }

            request.setAttribute("userName", user.getName());
            request.setAttribute("head", "/static/img/h.png");

        } catch (Throwable throwable) {
            throwable.printStackTrace();
            request.setAttribute("errStr", "不好意思啊，出错了。");
            return "err.jsp";
        }

        return "userInfo.jsp";
    }
}
