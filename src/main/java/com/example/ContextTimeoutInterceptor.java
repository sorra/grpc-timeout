/*
 * Copyright 2014 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class ContextTimeoutInterceptor implements ServerInterceptor {

  private final ContextTimeoutManager timeoutManager;

  public ContextTimeoutInterceptor(ContextTimeoutManager timeoutManager) {
    this.timeoutManager = timeoutManager;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> serverCall,
      Metadata metadata,
      ServerCallHandler<ReqT, RespT> serverCallHandler) {
    // Only intercepts unary calls because the timeout is inapplicable to streaming calls.
    if (serverCall.getMethodDescriptor().getType().clientSendsOneMessage()) {
      return new TimeoutServerCallListener<>(
              serverCallHandler.startCall(serverCall, metadata), timeoutManager);
    } else {
      return serverCallHandler.startCall(serverCall, metadata);
    }
  }

  /** A listener that intercepts the RPC method invocation for timeout control. */
  private static class TimeoutServerCallListener<ReqT>
      extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

    private final ContextTimeoutManager timeoutManager;

    private TimeoutServerCallListener(
        ServerCall.Listener<ReqT> delegate,
        ContextTimeoutManager timeoutManager) {
      super(delegate);
      this.timeoutManager = timeoutManager;
    }

    /**
     * Intercepts onHalfClose() because the RPC method is called in it. See
     * io.grpc.stub.ServerCalls.UnaryServerCallHandler.UnaryServerCallListener
     */
    @Override
    public void onHalfClose() {
      timeoutManager.withTimeout(super::onHalfClose);
    }
  }
}
