package com.lagou.edu.mvcframework.servlet;

import com.lagou.edu.annotations.*;
import com.lagou.edu.mvcframework.pojo.Handler;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: kevliu3
 * Date: 2020/9/18
 * Time: 11:44 AM
 *
 * @author kevliu3
 */
public class LagouDispatcherServlet extends HttpServlet {

    private static final long serialVersionUID = 8615285319613458729L;

    private Properties properties = new Properties();
    /**
     * 缓存扫描到的类的全限定类名
     */
    private List<String> classNames = new ArrayList<>();

    /**
     * IOC容器
     */
    private Map<String, Object> ioc = new HashMap<>();
    /**
     * url和method之间的映射关系
     */
//    private Map<String, Method> handlerMapping = new HashMap<>();
    List<Handler> handlerMapping = new ArrayList<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.加载配置文件 springmvc.properties
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2.扫描相关的类,扫描注解
        doScan(properties.getProperty("scanPackage"));

        //3.初始化bean对象(实现IOC容器,基于注解)s
        doInstance();

        //4.实现依赖注入
        doAutoWired();

        //5.构造一个HandlerMapping(处理器映射器),将配置好的url和method建立映射关系
        initHandlerMapping();
        System.out.println("lagou mvc 初始化完成....");
        //等待请求进入,处理请求
    }

    /**
     * 手写mvc最关键的环节
     * 目的:将url和method建立关联
     */
    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        //扫描所有的ioc容器对象的方法
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //获取ioc中当前遍历对象class的类型
            Class<?> aClass = entry.getValue().getClass();
            if (!aClass.isAnnotationPresent(LagouController.class)) {
                continue;
            }
            String baseUrl = "";
            if (aClass.isAnnotationPresent(LagouRequestMapping.class)) {
                LagouRequestMapping annotation = aClass.getAnnotation(LagouRequestMapping.class);
                baseUrl = annotation.value();
            }
            Method[] methods = aClass.getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                //没有标识requestmapping就不处理
                if (!method.isAnnotationPresent(LagouRequestMapping.class)) {
                    continue;
                }
                //处理
                LagouRequestMapping annotation = method.getAnnotation(LagouRequestMapping.class);
                //query
                String methodUrl = annotation.value();
                //计算出来的url
                String url = baseUrl + methodUrl;
                //建立url和method的映射关系,并缓存起来
                //把method所有信息及url封装成handler
                Handler handler = new Handler(entry.getValue(), method, Pattern.compile(url));
                //计算方法的参数位置信息
                Parameter[] parameters = method.getParameters();
                for (int j = 0; j < parameters.length; j++) {
                    Parameter parameter = parameters[j];
                    if (parameter.getType() == HttpServletRequest.class || parameter.getType() == HttpServletResponse.class) {
                        //如果是request或response对象
                        handler.getParamsIndexMapping().put(parameter.getType().getSimpleName(), j);
                    } else {
                        handler.getParamsIndexMapping().put(parameter.getName(), j);
                    }
                }
                handlerMapping.add(handler);
            }
        }
    }

    //实现依赖注入
    private void doAutoWired() {
        if (ioc.isEmpty()) {
            return;
        }
        //有对象,依赖注入处理
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //获取bean对象的字段信息
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(LagouAutowired.class)) {
                    continue;
                }
                //有该注解
                LagouAutowired annotation = field.getAnnotation(LagouAutowired.class);
                String beanName = annotation.value();
                if ("".equals(beanName.trim())) {
                    //没有配置具体的bean id,那就需要根据当前字段类型注入(接口注入)
                    beanName = field.getType().getName();
                }
                //开启赋值
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 基于classNames缓存的类的全限定类名以及反射技术完成对象创建和管理(IOC容器)
     */
    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        try {
            for (String className : classNames) {
                //反射创建对象
                Class<?> aClass = Class.forName(className);
                //区分controller和service
                if (aClass.isAnnotationPresent(LagouController.class)) {
                    //controller的id不做处理,不取value,拿类的首字母小写作为id,保存到IOC中
                    String simpleName = aClass.getSimpleName();
                    String lowerFirstSimpleName = lowerFirst(simpleName);
                    Object o = aClass.newInstance();
                    ioc.put(lowerFirstSimpleName, o);
                } else if (aClass.isAnnotationPresent(LagouService.class)) {
                    LagouService annotation = aClass.getAnnotation(LagouService.class);
                    //beanName
                    String beanName = annotation.value();
                    if (!"".equals(beanName.trim())) {
                        ioc.put(beanName, aClass.newInstance());
                    } else {
                        //没有指定,以类名首字母
                        beanName = lowerFirst(aClass.getSimpleName());
                        ioc.put(beanName, aClass.newInstance());
                    }

                    //service往往是有接口的,面向接口开发,以接口名为id,放一份对象到ioc中,便于后期按接口注入
                    Class<?>[] interfaces = aClass.getInterfaces();
                    for (Class<?> anInterface : interfaces) {
                        String name = anInterface.getName();
                        //以接口的类名作为id放入
                        ioc.put(name, aClass.newInstance());
                    }
                } else {
                    continue;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String lowerFirst(String str) {
        char[] chars = str.toCharArray();
        if ('A' <= chars[0] && 'Z' <= chars[0]) {
            chars[0] += 32;
        }
        return String.valueOf(chars);
    }

    /**
     * 扫描类
     *
     * @param scanPackage package:com.lagou.demo
     */
    private void doScan(String scanPackage) {
        String scanPackagePath = Thread.currentThread().getContextClassLoader().getResource("").getPath() + scanPackage.replaceAll("\\.", "/");
        File pack = new File(scanPackagePath);
        File[] files = pack.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            //子包
            if (file.isDirectory()) {
                //递归
                doScan(scanPackage + "." + file.getName());
            } else if (file.getName().endsWith("class")) {
                //全限定类名
                String className = scanPackage + "." + file.getName().replaceAll(".class", "");
                classNames.add(className);

            }
        }
    }

    /**
     * 加载配置文件
     */
    private void doLoadConfig(String contextConfigLocation) {
        try {
            properties.load(this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //处理请求：根据url，找到对应method进行调用
//        String requestURI = req.getRequestURI();
        //获取到反射方法
//        Method method = handlerMapping.get(requestURI);
        //此处无法完成调用,没有缓存对象,也没有参数.改造initHandlerMapping
//        method.invoke();
        //根据uri获取到能够处理当前请求的handler
        Handler handler = getHandler(req);
        if (handler == null) {
            resp.getWriter().write("404 not found");
            return;
        }
        String name = req.getParameter("name");
        if (!checkHandlerPermisson(name, handler)) {
            resp.getWriter().write("401 no permisson");
            return;
        }
        //参数绑定
        //获取所有参数类型数组,这个数组长度就是我们最后要传入的args数组的长度
        Class<?>[] parameterTypes = handler.getMethod().getParameterTypes();
        //根据上述数组长度创建一个新的数组
        Object[] paramValues = new Object[parameterTypes.length];
        //塞值,保证顺序一致
        Map<String, String[]> parameterMap = req.getParameterMap();
        //遍历所有参数
        for (Map.Entry<String, String[]> params : parameterMap.entrySet()) {
            // name=1&name=2 name[1,2]
            String value = StringUtils.join(params.getValue(), ",");
            //如果参数和方法中的参数匹配上了,填充数据
            if (!handler.getParamsIndexMapping().containsKey(params.getKey())) {
                continue;
            }
            //方法中确实有该参数,找到索引位置,放入paramvalue中
            Integer index = handler.getParamsIndexMapping().get(params.getKey());
            //填充到对应位置
            paramValues[index] = value;
        }
        //0
        Integer reuqestIndex = handler.getParamsIndexMapping().get(HttpServletRequest.class.getSimpleName());
        paramValues[reuqestIndex] = req;
        Integer responseIndex = handler.getParamsIndexMapping().get(HttpServletResponse.class.getSimpleName());
        paramValues[responseIndex] = resp;

        try {
            handler.getMethod().invoke(handler.getController(), paramValues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    private boolean checkHandlerPermisson(String name, Handler handler) {
        Object controller = handler.getController();
        Method method = handler.getMethod();
        LagouSecurity controllerSecurity = controller.getClass().getAnnotation(LagouSecurity.class);
        LagouSecurity methodSecurity = method.getAnnotation(LagouSecurity.class);
        if (!checkPermisson(name, controllerSecurity)) {
            return false;
        }
        if (!checkPermisson(name, methodSecurity)) {
            return false;
        }
        return true;
    }

    private boolean checkPermisson(String name, LagouSecurity security) {
        if (security == null || security.value().length == 0) {
            return true;
        }
        for (String val : security.value()) {
            if (val.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private Handler getHandler(HttpServletRequest req) {
        if (handlerMapping.isEmpty()) {
            return null;
        }
        String url = req.getRequestURI();
        for (Handler handler : handlerMapping) {
            Matcher matcher = handler.getPattern().matcher(url);
            if (!matcher.matches()) {
                continue;
            }
            return handler;
        }
        return null;
    }
}
