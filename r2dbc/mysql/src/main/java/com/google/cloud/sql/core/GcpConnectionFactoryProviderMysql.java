/*
 * Copyright 2020 Google Inc. All Rights Reserved.
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

import static io.r2dbc.spi.ConnectionFactoryOptions.Builder;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;

import io.asyncer.r2dbc.mysql.MySqlConnectionFactoryProvider;
import io.asyncer.r2dbc.mysql.constant.SslMode;
import io.netty.handler.ssl.SslContextBuilder;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryProvider;
import java.util.List;
import java.util.function.Function;

/** {@link ConnectionFactoryProvider} for proxied access to GCP MySQL instances. */
public class GcpConnectionFactoryProviderMysql extends GcpConnectionFactoryProvider {

  /** MySQL driver option value. */
  private static final String MYSQL_DRIVER = "mysql";

  static {
    CoreSocketFactory.addArtifactId("cloud-sql-connector-r2dbc-mysql");
  }

  @Override
  boolean supportedProtocol(String protocol) {
    return protocol.equals(MYSQL_DRIVER);
  }

  @Override
  ConnectionFactory tcpSocketConnectionFactory(
      Builder builder,
      String ipTypes,
      String targetPrincipal,
      List<String> delegates,
      Function<SslContextBuilder, SslContextBuilder> customizer,
      String hostname) {
    builder
        .option(MySqlConnectionFactoryProvider.SSL_CONTEXT_BUILDER_CUSTOMIZER, customizer)
        .option(MySqlConnectionFactoryProvider.SSL_MODE, SslMode.TUNNEL)
        .option(MySqlConnectionFactoryProvider.TCP_NO_DELAY, true)
        .option(MySqlConnectionFactoryProvider.TCP_KEEP_ALIVE, true);

    return new CloudSqlConnectionFactory(
        MySqlConnectionFactoryProvider::new,
        ipTypes,
        targetPrincipal,
        delegates,
        builder,
        hostname);
  }

  @Override
  ConnectionFactory unixSocketConnectionFactory(Builder optionBuilder, String socket) {
    optionBuilder.option(MySqlConnectionFactoryProvider.UNIX_SOCKET, socket);
    return new MySqlConnectionFactoryProvider().create(optionBuilder.build());
  }

  @Override
  Builder createBuilder(ConnectionFactoryOptions connectionFactoryOptions) {
    return connectionFactoryOptions.mutate().option(DRIVER, MYSQL_DRIVER);
  }
}
