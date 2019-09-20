package et.rpc.spring.annotation;

import et.rpc.spring.EtReferenceRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import(value = EtReferenceRegistrar.class)
public @interface EnableEt {
}
