package et.rpc.demo.hello.server;

import et.rpc.demo.hello.api.HelloService;
import et.rpc.server.spring.EtService;

import java.util.Date;

/**
 * @author wangl
 */
@EtService(HelloService.class)
public class HelloServiceImpl implements HelloService {

    @Override
    public String say(String name) {
       String msg = "hello " + name + " " + new Date();
        System.out.println(msg);
        return msg;
    }
}
