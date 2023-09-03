package com.example;

import com.example.helloworld.GreeterGrpc;
import com.example.helloworld.HelloReply;
import com.example.helloworld.HelloRequest;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server2 {

    public static void main(String[] args) throws Exception {
        startGrpcServer(Executors.newCachedThreadPool()).awaitTermination();
    }

    private static io.grpc.Server startGrpcServer(ExecutorService serverExecutor) throws IOException {
        io.grpc.Server server = Grpc.newServerBuilderForPort(50052, InsecureServerCredentials.create())
                .executor(serverExecutor)
                .addService(new Greeter2Impl())
                .build()
                .start();

        System.out.println("Server2 started.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
                System.out.println("Server2 shutdown.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
        return server;
    }

    public static class Greeter2Impl extends GreeterGrpc.GreeterImplBase {

        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            System.out.println("Greeter2 is called");
            if (Thread.interrupted()) {
                responseObserver.onError(
                    new StatusRuntimeException(Status.CANCELLED.withDescription("Greeter2 thread interrupted")));
                return;
            }
            System.out.println("Greeter2 not interrupted.");

            HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + request.getName()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }
}
