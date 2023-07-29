package com.example;

import com.example.helloworld.GreeterGrpc;
import com.example.helloworld.HelloReply;
import com.example.helloworld.HelloRequest;
import io.grpc.Channel;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

public class Client {

    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    public Client(Channel channel) {
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    public static void main(String[] args) {
        ManagedChannel channel = Grpc.newChannelBuilder("localhost:50051", InsecureChannelCredentials.create())
                .build();
        new Client(channel).greet("Timer");
    }

    public void greet(String name) {
        System.out.println("Will try to greet " + name + " ...");
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        try {
            response = blockingStub.sayHello(request);
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("Greeting: " + response.getMessage());
    }
}
