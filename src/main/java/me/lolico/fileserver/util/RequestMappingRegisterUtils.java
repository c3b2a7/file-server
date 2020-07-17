package me.lolico.fileserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * @author lolico
 */
public class RequestMappingRegisterUtils {
    private static final Logger logger = LoggerFactory.getLogger(RequestMappingRegisterUtils.class);
    private static final Method DETECT_HANDLER_METHODS;
    private static final Method GET_MAPPING_FOR_METHOD;
    private static final Field CONFIG_FIELD;

    static {
        try {
            DETECT_HANDLER_METHODS = AbstractHandlerMethodMapping.class.getDeclaredMethod("detectHandlerMethods", Object.class);
            GET_MAPPING_FOR_METHOD = AbstractHandlerMethodMapping.class.getDeclaredMethod("getMappingForMethod", Method.class, Class.class);
            CONFIG_FIELD = RequestMappingHandlerMapping.class.getDeclaredField("config");
        } catch (NoSuchMethodException | NoSuchFieldException ex) {
            // Should never happen
            throw new IllegalStateException("Failed to retrieve internal handler method", ex);
        }
    }

    public static boolean registerMapping(RequestMappingHandlerMapping requestMappingHandlerMapping,
                                          String path,
                                          RequestMethod requestMethod,
                                          Object handle,
                                          Method handleMethod) {
        try {
            ReflectionUtils.makeAccessible(CONFIG_FIELD);
            RequestMappingInfo.BuilderConfiguration builderConfiguration =
                    (RequestMappingInfo.BuilderConfiguration)
                            ReflectionUtils.getField(CONFIG_FIELD, requestMappingHandlerMapping);
            if (builderConfiguration == null) {
                // Should never happen
                throw new IllegalStateException("Failed to retrieve internal handler method");
            }
            RequestMappingInfo requestMappingInfo = RequestMappingInfo
                    .paths(path).methods(requestMethod)
                    .options(builderConfiguration)
                    .build();
            requestMappingHandlerMapping.registerMapping(requestMappingInfo, handle, handleMethod);
        } catch (Exception ex) {
            logger.warn("Occur exception during register mapping", ex);
            return false;
        }
        return true;
    }

    public static <T> boolean registerController(RequestMappingHandlerMapping requestMappingHandlerMapping,
                                                 BeanDefinitionRegistry beanDefinitionRegistry,
                                                 String beanName, Class<T> beanClass, Supplier<T> beanSupplier) {
        try {
            if (!isHandler(beanClass)) {
                return false;
            }
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(beanClass, beanSupplier);
            beanDefinitionRegistry.registerBeanDefinition(beanName, builder.getBeanDefinition());
            ReflectionUtils.makeAccessible(DETECT_HANDLER_METHODS);
            ReflectionUtils.invokeMethod(DETECT_HANDLER_METHODS, requestMappingHandlerMapping, beanName);
        } catch (Exception ex) {
            logger.warn("Occur exception during register mapping", ex);
            return false;
        }
        return true;
    }

    private static boolean isHandler(Class<?> beanType) {
        return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
                AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
    }

    public static boolean removeController(RequestMappingHandlerMapping requestMappingHandlerMapping,
                                           Object controller) {
        try {
            Class<?> targetClass = ClassUtils.getUserClass(controller);
            ReflectionUtils.doWithMethods(targetClass, method -> {
                Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
                ReflectionUtils.makeAccessible(GET_MAPPING_FOR_METHOD);
                RequestMappingInfo requestMappingInfo = (RequestMappingInfo) ReflectionUtils.invokeMethod(
                        GET_MAPPING_FOR_METHOD, requestMappingHandlerMapping, specificMethod, targetClass);
                if (requestMappingInfo != null) {
                    requestMappingHandlerMapping.unregisterMapping(requestMappingInfo);
                }
            }, method -> ReflectionUtils.USER_DECLARED_METHODS.matches(method) &&
                    AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class));
        } catch (Exception ex) {
            logger.warn("Occur exception during register mapping", ex);
            return false;
        }
        return true;
    }
}
