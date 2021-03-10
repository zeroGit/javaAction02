package org.geektimes.web.mvc;

import org.apache.commons.lang.StringUtils;
import org.geektimes.web.mvc.context.ComponentContext;
import org.geektimes.web.mvc.controller.Controller;
import org.geektimes.web.mvc.controller.PageController;
import org.geektimes.web.mvc.controller.RestController;
import org.geektimes.web.mvc.function.ThrowableFunction;
import org.geektimes.web.mvc.validator.ValidateRequest;

import javax.annotation.Resource;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.substringAfter;

public class FrontControllerServlet extends HttpServlet {

    @Resource(name = "bean/Validator")
    Validator validator;

    /**
     * 请求路径和 Controller 的映射关系缓存
     */
    private Map<String, Controller> controllersMapping = new HashMap<>();

    private ServletContext servletContext;

    /**
     * 请求路径和 {@link HandlerMethodInfo} 映射关系缓存
     */
    private Map<String, HandlerMethodInfo> handleMethodInfoMapping = new HashMap<>();

    private final String ErrPage = "err.jsp";

    private Map<String, ThrowableFunction<String, Object>> convertFuncMap;

    /**
     * 初始化 Servlet
     *
     * @param servletConfig
     */
    public void init(ServletConfig servletConfig) {

        convertFuncMap = new HashMap<>();
        convertFuncMap.put("java.lang.Long", Long::valueOf);
        convertFuncMap.put("java.lang.Integer", Integer::valueOf);

        this.servletContext = servletConfig.getServletContext();
        initHandleMethods();

        // controller 中的 Resource 的注入
        // 主要是 Validator
        ComponentContext attribute = (ComponentContext) this.servletContext.getAttribute(ComponentContext.CONTEXT_NAME);

        if (attribute != null) {
            controllersMapping.forEach((k, v) -> {
                attribute.injectComponents(v, v.getClass());
            });

            attribute.injectComponents(this, this.getClass());
        }

    }

    /**
     * 读取所有的 RestController 的注解元信息 @Path
     * 利用 ServiceLoader 技术（Java SPI）
     */
    private void initHandleMethods() {
        Map<String, Object> ctls = new HashMap<>();
        for (Controller controller : ServiceLoader.load(Controller.class)) {
            Class<?> controllerClass = controller.getClass();
            Path pathFromClass = controllerClass.getAnnotation(Path.class);
            String requestPath = pathFromClass.value();
            Method[] publicMethods = controllerClass.getMethods();
            // 处理方法支持的 HTTP 方法集合
            for (Method method : publicMethods) {
                Set<String> supportedHttpMethods = findSupportedHttpMethods(method);
                Path pathFromMethod = method.getAnnotation(Path.class);
                if (pathFromMethod != null) {
                    requestPath += pathFromMethod.value();
                }
                handleMethodInfoMapping.put(requestPath,
                        new HandlerMethodInfo(requestPath, method, supportedHttpMethods));
            }
            controllersMapping.put(requestPath, controller);
            ctls.put(controller.getClass().getName(), controller);
        }

        if (!ctls.isEmpty()) {
            this.servletContext.setAttribute("controllers", ctls);
        }
    }

    /**
     * 获取处理方法中标注的 HTTP方法集合
     *
     * @param method 处理方法
     * @return
     */
    private Set<String> findSupportedHttpMethods(Method method) {
        Set<String> supportedHttpMethods = new LinkedHashSet<>();
        for (Annotation annotationFromMethod : method.getAnnotations()) {
            HttpMethod httpMethod = annotationFromMethod.annotationType().getAnnotation(HttpMethod.class);
            if (httpMethod != null) {
                supportedHttpMethods.add(httpMethod.value());
            }
        }

        if (supportedHttpMethods.isEmpty()) {
            supportedHttpMethods.addAll(asList(HttpMethod.GET, HttpMethod.POST,
                    HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.HEAD, HttpMethod.OPTIONS));
        }

        return supportedHttpMethods;
    }

    /**
     * SCWCD
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 建立映射关系
        // requestURI = /a/hello/world
        String requestURI = request.getRequestURI();
        // contextPath  = /a or "/" or ""
        String servletContextPath = request.getContextPath();
        String prefixPath = servletContextPath;
        // 映射路径（子路径）
        String requestMappingPath = substringAfter(requestURI,
                StringUtils.replace(prefixPath, "//", "/"));
        // 映射到 Controller
        Controller controller = controllersMapping.get(requestMappingPath);

        if (controller != null) {

            HandlerMethodInfo handlerMethodInfo = handleMethodInfoMapping.get(requestMappingPath);

            try {
                if (handlerMethodInfo != null) {

                    String httpMethod = request.getMethod();

                    if (!handlerMethodInfo.getSupportedHttpMethods().contains(httpMethod)) {
                        // HTTP 方法不支持
                        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        return;
                    }

                    if (controller instanceof PageController) {
                        PageController pageController = PageController.class.cast(controller);

                        String viewPath = ErrPage;
                        // 请求参数验证，请求参数->对象 验证
                        boolean valid = validateRequest(pageController, request);
                        if (valid) {
                            viewPath = pageController.execute(request, response);
                        }

                        // 页面请求 forward
                        // request -> RequestDispatcher forward
                        // RequestDispatcher requestDispatcher = request.getRequestDispatcher(viewPath);
                        // ServletContext -> RequestDispatcher forward
                        // ServletContext -> RequestDispatcher 必须以 "/" 开头
                        ServletContext servletContext = request.getServletContext();
                        if (!viewPath.startsWith("/")) {
                            viewPath = "/" + viewPath;
                        }
                        RequestDispatcher requestDispatcher = servletContext.getRequestDispatcher(viewPath);
                        requestDispatcher.forward(request, response);
                        return;
                    } else if (controller instanceof RestController) {
                        // TODO
                    }

                }
            } catch (Throwable throwable) {
                if (throwable.getCause() instanceof IOException) {
                    throw (IOException) throwable.getCause();
                } else {
                    throw new ServletException(throwable.getCause());
                }
            }
        }
    }

    private boolean validateRequest(Object controller, HttpServletRequest request) {
        try {
            Method execute = controller.getClass().getMethod("execute", HttpServletRequest.class, HttpServletResponse.class);
            ValidateRequest annotation = execute.getAnnotation(ValidateRequest.class);

            Class<?> c = annotation.targetClass();
            Object o = c.newInstance();
            Field[] fields = c.getDeclaredFields();
            for (int i = fields.length - 1; i >= 0; i--) {
                Field f = fields[i];
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                String name = f.getName();
                String parameter = request.getParameter(name);
                if (parameter != null) {

                    Class<?> type = f.getType();
                    if (type.equals(java.lang.String.class)) {
                        f.setAccessible(true);
                        f.set(o, parameter);
                    } else {
                        ThrowableFunction<String, Object> func = convertFuncMap.get(type.getCanonicalName());
                        if (func != null) {
                            f.setAccessible(true);
                            f.set(o, ThrowableFunction.execute(parameter, func));
                        }
                    }
                }
            }

            Set<ConstraintViolation<Object>> validateR = validator.validate(o);
            if (validateR != null && validateR.size() > 0) {
                String[] errs = {""};

                validateR.forEach(v -> {
                    errs[0] += "[" + v.getMessage() + "] ";
                });
                request.setAttribute("errStr", errs[0]);
                return false;
            }

            request.setAttribute("requestObj", o);

        } catch (NoSuchMethodException | IllegalAccessException | NullPointerException | InstantiationException e) {
            return false;
        }

        return true;
    }

//    private void beforeInvoke(Method handleMethod, HttpServletRequest request, HttpServletResponse response) {
//
//        CacheControl cacheControl = handleMethod.getAnnotation(CacheControl.class);
//
//        Map<String, List<String>> headers = new LinkedHashMap<>();
//
//        if (cacheControl != null) {
//            CacheControlHeaderWriter writer = new CacheControlHeaderWriter();
//            writer.write(headers, cacheControl.value());
//        }
//    }
}
