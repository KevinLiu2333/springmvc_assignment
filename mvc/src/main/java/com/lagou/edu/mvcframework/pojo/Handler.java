package com.lagou.edu.mvcframework.pojo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: liuku
 * Date: 2020/9/19
 * Time: 10:37
 *
 * @author liuku
 */
public class Handler {
    private Object controller;

    private Method method;

    private Pattern pattern;

    private Map<String, Integer> paramsIndexMapping;

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Map<String, Integer> getParamsIndexMapping() {
        return paramsIndexMapping;
    }

    public void setParamsIndexMapping(Map<String, Integer> paramsIndexMapping) {
        this.paramsIndexMapping = paramsIndexMapping;
    }

    public Handler(Object controller, Method method, Pattern pattern) {
        this.controller = controller;
        this.method = method;
        this.pattern = pattern;
        this.paramsIndexMapping = new HashMap<>();
    }
}
