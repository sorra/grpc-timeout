package com.example;

import com.example.helloworld.GreeterGrpc;
import com.example.helloworld.HelloReply;
import com.example.helloworld.HelloRequest;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {

    public static void main(String[] args) throws Exception {
        ServerTimeoutManager manager = new ServerTimeoutManager(200, TimeUnit.MILLISECONDS, null);
        ServerInterceptor interceptor = new ServerCallTimeoutInterceptor(manager);

        io.grpc.Server server = Grpc.newServerBuilderForPort(50051, InsecureServerCredentials.create())
                .executor(Executors.newSingleThreadExecutor())
                .addService(new GreeterImpl())
                .intercept(interceptor)
                .build()
                .start();

        System.out.println("Server started.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                manager.shutdown();
                server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
                System.out.println("Server shutdown.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        server.awaitTermination();
    }

    public static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            try {
                Thread.sleep(202);
                HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + request.getName()).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (InterruptedException e) {
                responseObserver.onError(new StatusRuntimeException(Status.ABORTED));
            }
        }
    }
}
