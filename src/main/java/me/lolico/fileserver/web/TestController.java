package me.lolico.fileserver.web;

import me.lolico.fileserver.util.FileUtils;
import me.lolico.fileserver.util.RequestMappingRegisterUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.File;

/**
 * @author lolico
 */
@RestController
public class TestController implements BeanFactoryAware {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private BeanFactory beanFactory;

    public TestController(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    @GetMapping("/cc")
    public ResponseEntity<Object> createController(String path) {
        FileUtils fileUtils = new FileUtils(new File("uploads"), "*.*", path, false);
        boolean res = RequestMappingRegisterUtils.registerController(requestMappingHandlerMapping,
                ((DefaultListableBeanFactory) beanFactory),
                "uploadController", UploadController.class, () -> new UploadController(fileUtils));
        return ResponseEntity.ok(res);
    }

    @GetMapping("/cep")
    public ResponseEntity<Object> createEndpoint(String path) {
        FileUtils fileUtils = new FileUtils(new File("uploads"), "*.*", path, false);
        boolean res = RequestMappingRegisterUtils.registerMapping(
                requestMappingHandlerMapping,
                path,
                RequestMethod.POST,
                new UploadController(fileUtils),
                ReflectionUtils.findMethod(UploadController.class, "upload", MultipartFile.class)
        );
        return ResponseEntity.ok(res);
    }

    @GetMapping("/rc")
    public ResponseEntity<Object> removeController(String beanName) {
        try {
            Object bean = beanFactory.getBean(beanName);
            boolean res = RequestMappingRegisterUtils.removeController(requestMappingHandlerMapping, bean);
            return ResponseEntity.ok(res);
        } catch (BeansException e) {
            return ResponseEntity.ok(false);
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
