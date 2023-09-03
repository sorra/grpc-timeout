package com.example;

import com.example.helloworld.GreeterGrpc;
import com.example.helloworld.HelloReply;
import com.example.helloworld.HelloRequest;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.InsecureServerCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ServerInterceptor;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Server {

    public static void main(String[] args) throws Exception {
        Consumer<String> logger = msg -> System.out.println("Server: " + msg);
        int timeoutMillis = 100;
        ServerInterceptor interceptor;
        if (args.length > 0) {
            // My way
            var timeoutManager = new ServerTimeoutManager(timeoutMillis, TimeUnit.MILLISECONDS, logger);
            Runtime.getRuntime().addShutdownHook(new Thread(timeoutManager::shutdown));
            interceptor = new ServerCallTimeoutInterceptor(timeoutManager);
        } else {
            // Officially recommended way
            var timeoutManager = new ContextTimeoutManager(timeoutMillis, TimeUnit.MILLISECONDS, logger);
            Runtime.getRuntime().addShutdownHook(new Thread(timeoutManager::shutdown));
            interceptor = new ContextTimeoutInterceptor(timeoutManager);
        }

        startGrpcServer(Executors.newFixedThreadPool(2), interceptor).awaitTermination();
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
        private final GreeterGrpc.GreeterBlockingStub blockingStub;
        {
            // Server2 at 50052
            ManagedChannel channel = Grpc.newChannelBuilder("localhost:50052", InsecureChannelCredentials.create())
                .build();
            blockingStub = GreeterGrpc.newBlockingStub(channel);
        }

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            System.out.println("Greeter is called");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Greeter not interrupted.");

            HelloReply reply;
            try {
                reply = blockingStub.sayHello(request);
            } catch (StatusRuntimeException e) {
                responseObserver.onError(e);
                return;
            }
            System.out.println("subsequent Greeter2 call not interrupted.");

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}
