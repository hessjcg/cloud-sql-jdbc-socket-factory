/*
 * Copyright 2023 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.sql.core;

import com.google.cloud.sql.AuthType;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import java.security.KeyPair;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

class TestDataSupplier implements InstanceDataSupplier {

  volatile boolean flaky;

  final AtomicInteger counter = new AtomicInteger();
  final AtomicInteger successCounter = new AtomicInteger();
  final InstanceData response =
      new InstanceData(
          new Metadata(
              ImmutableMap.of(
                  "PUBLIC", "10.1.2.3",
                  "PRIVATE", "10.10.10.10",
                  "PSC", "abcde.12345.us-central1.sql.goog"),
              null),
          new SslData(null, null, null),
          Date.from(Instant.now().plus(1, ChronoUnit.HOURS)));

  TestDataSupplier(boolean flaky) {
    this.flaky = flaky;
  }

  @Override
  public InstanceData getInstanceData(
      CloudSqlInstanceName instanceName,
      AccessTokenSupplier accessTokenSupplier,
      AuthType authType,
      ListeningScheduledExecutorService executor,
      ListenableFuture<KeyPair> keyPair)
      throws ExecutionException, InterruptedException {

    // This method mimics the behavior of SqlAdminApiFetcher under flaky network conditions.
    // It schedules a future on the executor to produces the result InstanceData.
    // When `this.flaky` is set, every other call to getInstanceData()
    // throw an ExecutionException, as if SqlAdminApiFetcher made an API request,
    // and then failed.
    ListenableFuture<InstanceData> f =
        executor.submit(
            () -> {
              Thread.sleep(100);
              int c = counter.incrementAndGet();
              if (flaky && c % 2 == 0) {
                throw new ExecutionException("Flaky", new Exception());
              }
              successCounter.incrementAndGet();
              return response;
            });

    return f.get();
  }
}
