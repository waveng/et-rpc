package et.rpc.client;

import et.rpc.common.bean.EtRpcResponse;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

class Response {
    private String requestId;
    private EtRpcResponse etRpcResponse;
    private Thread thread;
    private AtomicBoolean isPark = new AtomicBoolean(false);

    public Response(String requestId){
        this.requestId = requestId;
        thread = Thread.currentThread();
    }

    public EtRpcResponse get(){
        if(etRpcResponse != null){
            return etRpcResponse;
        }
        isPark.compareAndSet(false,true);
        LockSupport.park();
        return etRpcResponse;
    }

    public void set(EtRpcResponse etRpcResponse){
        this.etRpcResponse = etRpcResponse;
        if(isPark.compareAndSet(true, false)) {
            LockSupport.unpark(thread);
        }
    }
}
