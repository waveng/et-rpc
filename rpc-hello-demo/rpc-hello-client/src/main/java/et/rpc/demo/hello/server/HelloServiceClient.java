package et.rpc.demo.hello.server;

import et.rpc.demo.hello.api.HelloService;
import et.rpc.spring.annotation.EtReference;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class HelloServiceClient {
    @EtReference(value= HelloService.class)
    private HelloService helloService;

    @PostConstruct
    public void run(){
        while (true) {
            System.out.println(helloService.say("wored"));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //
            }
        }
    }
}
