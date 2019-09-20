package et.rpc.demo.hello;


import et.rpc.spring.annotation.EnableEt;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableEt
@SpringBootApplication
public class ClientHelloApplication {
    public static void main(String[] args){
        SpringApplication.run(ClientHelloApplication.class, args);
    }
}
