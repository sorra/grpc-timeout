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

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {

    public static void main(String[] args) throws Exception {
        ExecutorService serverExecutor = Executors.newFixedThreadPool(2);

        ServerInterceptor interceptor;
        if (args.length == 0) {
            var timeoutManager = new ServerTimeoutManager(100, TimeUnit.MILLISECONDS, null);
            Runtime.getRuntime().addShutdownHook(new Thread(timeoutManager::shutdown));
            interceptor = new ServerCallTimeoutInterceptor(timeoutManager);
        } else {
            var timeoutManager = new CancellableTimeoutManager(100, TimeUnit.MILLISECONDS);
            Runtime.getRuntime().addShutdownHook(new Thread(timeoutManager::shutdown));
            interceptor = new CancellableTimeoutInterceptor(timeoutManager, serverExecutor);
        }

        startGrpcServer(serverExecutor, interceptor).awaitTermination();
    }

    private static io.grpc.Server startGrpcServer(ExecutorService serverExecutor, ServerInterceptor interceptor) throws IOException {
        io.grpc.Server server = Grpc.newServerBuilderForPort(50051, InsecureServerCredentials.create())
                .executor(serverExecutor)
                .addService(new GreeterImpl())
                .intercept(interceptor)
                .build()
                .start();

        System.out.println("Server started.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
                System.out.println("Server shutdown.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
        return server;
    }

    public static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                responseObserver.onError(new StatusRuntimeException(Status.ABORTED));
            }
            System.out.println("Not interrupted.");

            HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + request.getName()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}
