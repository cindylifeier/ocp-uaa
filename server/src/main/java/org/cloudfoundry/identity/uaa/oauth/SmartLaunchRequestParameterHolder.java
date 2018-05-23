package org.cloudfoundry.identity.uaa.oauth;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public class SmartLaunchRequestParameterHolder {

    public static final String LAUNCH = "launch";
    public static final String CODE = "code";
    public static final String DELIMITER = ":";

    public static Optional<HttpServletRequest> getRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest);
    }

    public static Optional<String> getLaunchFromCodeRequestParam() {
        return getCodeParam()
                .filter(code -> {
                    final int i = code.indexOf(DELIMITER);
                    return i > 0 && i + 1 < code.length();
                })
                .map(code -> code.substring(code.indexOf(DELIMITER) + 1));
    }

    public static Optional<String> getLaunchRequestParam() {
        return getLaunchRequestParam(getRequest());
    }

    public static Optional<String> getLaunchRequestParam(Optional<HttpServletRequest> httpServletRequest) {
        return getParamOne(httpServletRequest, LAUNCH);
    }

    public static Optional<String> getCodeParam() {
        return getCodeParam(getRequest());
    }

    public static Optional<String> getCodeParam(Optional<HttpServletRequest> httpServletRequest) {
        return getParamOne(httpServletRequest, CODE);
    }

    public static Optional<String> getParamOne(Optional<HttpServletRequest> httpServletRequest, String name) {
        return httpServletRequest
                .map(request -> request.getParameterValues(name))
                .filter(param -> param.length == 1)
                .map(param -> param[0]);
    }

    public static Optional<String[]> getParamAll(Optional<HttpServletRequest> httpServletRequest, String name) {
        return httpServletRequest
                .map(request -> request.getParameterValues(name));
    }
}
