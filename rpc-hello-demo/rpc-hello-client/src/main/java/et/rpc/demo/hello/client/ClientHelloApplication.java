package et.rpc.demo.hello.client;

import et.rpc.client.EtRpcClient;
import et.rpc.demo.hello.api.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class ClientHelloApplication {

    @Autowired
    private EtRpcClient etRpcClient;

    @PostConstruct
    public void run(){
        HelloService helloService = etRpcClient.create(HelloService.class);
        while (true) {
            System.out.println(helloService.say("wored"));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //
            }
        }
    }
    public static void main(String[] args){
        SpringApplication.run(ClientHelloApplication.class, args);
    }
}
