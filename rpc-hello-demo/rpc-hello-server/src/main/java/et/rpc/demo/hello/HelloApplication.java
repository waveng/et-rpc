package et.rpc.demo.hello;

import et.rpc.spring.annotation.EnableEt;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableEt
@SpringBootApplication
public class HelloApplication {
    public static void main(String[] args){
        SpringApplication.run(HelloApplication.class, args);
    }
}
