# Connecting to MySQL using JDBC

## Setup and Usage

### Adding the library as a dependency

**Note**: Use your JDBC driver version to figure out which SocketFactory you should use. If you 
are unsure, it is recommended to use the latest version of `mysql-connector-java:8.x`.

| JDBC Driver Version        | Cloud SQL Socket Factory Version         |
| -------------------------- | ---------------------------------------- |
| mysql-connector-java:8.x   | mysql-socket-factory-connector-j-8:1.13.1 |
| mysql-connector-java:5.1.x | mysql-socket-factory:1.13.1            |

##### Maven
Include the following in the project's `pom.xml`: 
```maven-pom
<dependency>
    <groupId>com.google.cloud.sql</groupId>
    <artifactId>mysql-socket-factory-connector-j-8</artifactId>
    <version>1.13.1</version>
</dependency>
```

##### Gradle
Include the following the project's `build.gradle`
```gradle
compile 'com.google.cloud.sql:mysql-socket-factory-connector-j-8:1.13.1'
```

### Creating theJDBC URL

Base JDBC URL: `jdbc:mysql:///<DATABASE_NAME>`

When specifying the JDBC connection URL, add the additional parameters:

| Property         | Value         |
| ---------------- | ------------- |
| socketFactory    | com.google.cloud.sql.mysql.SocketFactory |
| cloudSqlInstance | The instance connection name (found on the instance details page) |
| user             | MySQL username |
| password         | MySQL user's password |

The full JDBC URL should look like this:
```
jdbc:mysql:///<DATABASE_NAME>?cloudSqlInstance=<INSTANCE_CONNECTION_NAME>&socketFactory=com.google.cloud.sql.mysql.SocketFactory&user=<MYSQL_USER_NAME>&password=<MYSQL_USER_PASSWORD>
```

Note: The host portion of the JDBC URL is currently unused, and has no effect on the connection process. The SocketFactory will get your instances IP address based on the provided `cloudSqlInstance` arg. 

### Specifying IP Types

"The `ipTypes` argument is used to specify a preferred order of IP types used
to connect via a comma delimited list. For example, `ipTypes=PUBLIC,PRIVATE`
will use the instance's Public IP if it exists, otherwise private. The
value `ipTypes=PRIVATE` will force the Cloud SQL instance to connect via
it's private IP. The value `ipTypes=PSC` will force the Cloud SQL instance to
connect to the database via [Private Service Connect](https://cloud.google.com/vpc/docs/private-service-connect).
If not specified, the default used is `ipTypes=PUBLIC,PRIVATE`.

For more info on connecting using a private IP address, see [Requirements for Private IP](https://cloud.google.com/sql/docs/mysql/private-ip#requirements_for_private_ip).

### IAM Authentication
*Note:* This feature is currently only supported for MySQL j8 and Postgres drivers.
Connections using 
[IAM database authentication](https://cloud.google.com/sql/docs/postgres/iam-logins) 
are supported when connecting to Postgres instances.
This feature is unsupported for other drivers. First, make sure to
[configure your Cloud SQL Instance to allow IAM authentication](https://cloud.google.com/sql/docs/mysql/create-edit-iam-instances#configure-iam-db-instance)
and
[add an IAM database user](https://cloud.google.com/sql/docs/mysql/create-manage-iam-users#creating-a-database-user).
Now, you can connect using user or service
account credentials instead of a password. 
When setting up the connection, set the `enableIamAuth` connection property to `"true"` and `user`
to the part of the email address associated with your IAM user before the @ symbol. For a service account, this is the service account's email address without the @project-id.iam.gserviceaccount.com suffix

Example:
```java
    // Set up URL parameters
    String jdbcURL = String.format("jdbc:mysql:///%s", DB_NAME);
    Properties connProps = new Properties();
    connProps.setProperty("user", "iam-user"); // iam-user@gmail.com
    connProps.setProperty("sslmode", "disable");
    connProps.setProperty("socketFactory", "com.google.cloud.sql.mysql.SocketFactory");
    connProps.setProperty("cloudSqlInstance", "project:region:instance");
    connProps.setProperty("enableIamAuth", "true");

    // Initialize connection pool
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(jdbcURL);
    config.setDataSourceProperties(connProps);
    config.setConnectionTimeout(10000); // 10s

    HikariDataSource connectionPool = new HikariDataSource(config);
```

### Service Account Impersonation

The Java Connector supports service account impersonation with the
`cloudSqlTargetPrincipal` JDBC connection property. When enabled,
all API requests are made impersonating the supplied service account. The
IAM principal must have the iam.serviceAccounts.getAccessToken permission or
the role roles/iam.serviceAccounts.serviceAccountTokenCreator.

Example:
```java
// Set up URL parameters
String jdbcURL = String.format("jdbc:mysql:///%s", DB_NAME);
Properties connProps = new Properties();
connProps.setProperty("user", "iam-user"); // iam-user@gmail.com
connProps.setProperty("sslmode", "disable");
connProps.setProperty("socketFactory", "com.google.cloud.sql.postgres.SocketFactory");
connProps.setProperty("cloudSqlInstance", "project:region:instance");
connProps.setProperty("enableIamAuth", "true");
connProps.setProperty("cloudSqlTargetPrincipal", "iam-user@gmail.com");

// Initialize connection pool
HikariConfig config = new HikariConfig();
config.setJdbcUrl(jdbcURL);
config.setDataSourceProperties(connProps);
config.setConnectionTimeout(10000); // 10s

HikariDataSource connectionPool = new HikariDataSource(config);
```

In addition, the `cloudSqlDelegates` property controls impersonation delegation.
The value is a comma-separated list of service accounts containing chained
list of delegates required to grant the final access_token. If set,
the sequence of identities must have "Service Account Token Creator" capability
granted to the preceding identity. For example, if set to 
`"serviceAccountB,serviceAccountC"`, the application default credentials must
have the Token Creator role on serviceAccountB. serviceAccountB must have
the Token Creator on serviceAccountC. Finally, C must have Token Creator on
`cloudSqlTargetPrincipal`. If unset, the application default credential principal
must "Service Account Token Creator" capability granted that role on the
`cloudSqlTargetPrincipal` service account.


For example:
```java
connProps.setProperty("cloudSqlTargetPrincipal", "TARGET_SERVICE_ACCOUNT");
connProps.setProperty("cloudSqlDelegates", "SERVICE_ACCOUNT_1,SERVICE_ACCOUNT_2");
```

In this example, the environment's application default principal impersonates
SERVICE_ACCOUNT_1 which impersonates SERVICE_ACCOUNT_2 which then
impersonates the TARGET_SERVICE_ACCOUNT.


### Connection via Unix Sockets

To connect using a Unix domain socket (such as the one created by the Cloud SQL 
proxy), you can use the `unixSocketPath` property to specify a path to a local 
file instead of connecting directly over TCP.

```
jdbc:mysql:///<DATABASE_NAME>?unixSocketPath=</PATH/TO/UNIX/SOCKET>&cloudSqlInstance=<INSTANCE_CONNECTION_NAME>&socketFactory=com.google.cloud.sql.mysql.SocketFactory&user=<MYSQL_USER_NAME>&password=<MYSQL_USER_PASSWORD>
```

## Examples

Examples for using the Cloud SQL JDBC Connector for MySQL can be found by looking at the integration tests in this repository.
* [Usage example with connector-j-5](../jdbc/mysql-j-5/src/test/java/com/google/cloud/sql/mysql/JdbcMysqlJ5IntegrationTests.java)
* [Usage example with connector-j-8](../jdbc/mysql-j-8/src/test/java/com/google/cloud/sql/mysql/JdbcMysqlJ8IntegrationTests.java)

## Reference Documentation
* [Connecting to Cloud SQL from App Engine Standard](https://cloud.google.com/sql/docs/mysql/connect-app-engine-standard)
* [Connecting to Cloud SQL from App Engine Flexible](https://cloud.google.com/sql/docs/mysql/connect-app-engine-flexible)
* [Connecting to Cloud SQL from Cloud Functions](https://cloud.google.com/sql/docs/mysql/connect-functions)
* [Connecting to Cloud SQL from Cloud Run](https://cloud.google.com/sql/docs/mysql/connect-run)
