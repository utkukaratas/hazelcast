/*
 * Copyright 2023 Hazelcast Inc.
 *
 * Licensed under the Hazelcast Community License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://hazelcast.com/hazelcast-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hazelcast.jet.mongodb.dataconnection;

import com.hazelcast.config.DataConnectionConfig;
import com.hazelcast.dataconnection.DataConnectionBase;
import com.hazelcast.dataconnection.DataConnectionResource;
import com.hazelcast.spi.annotation.Beta;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.hazelcast.internal.util.Preconditions.checkState;
import static com.hazelcast.jet.mongodb.impl.Mappers.defaultCodecRegistry;
import static java.util.Collections.singletonList;

/**
 * Creates a MongoDB DataConnection.
 * <p>
 * According to {@link MongoClient} documentation, the client object is responsible for maintaining connection pool,
 * so this data connection just returns one, cached client.
 *
 * @since 5.3
 */
@Beta
public class MongoDataConnection extends DataConnectionBase {
    /**
     * Name of a property which holds connection string to the mongodb instance.
     */
    public static final String CONNECTION_STRING_PROPERTY = "connectionString";
    /**
     * Name of a property with a database name hint.
     * This is used as a <strong>hint</strong> only; {@link #listResources} will return only collection from db with this
     * name, but client is not restricted to this database. It can be used to specify database to which SQL Mappings
     * will point.
     */
    public static final String DATABASE_PROPERTY = "database";

    /**
     * Name of the property holding username.
     */
    public static final String USERNAME_PROPERTY = "username";
    /**
     * Name of the property holding user password.
     */
    public static final String PASSWORD_PROPERTY = "password";
    /**
     * Name of a property which holds host:port address of the mongodb instance.
     */
    public static final String HOST_PROPERTY = "host";
    /**
     * Name of the property holding the name of the database in which user is created.
     * Default value is {@code admin}.
     */
    public static final String AUTH_DB_PROPERTY = "authDb";

    private volatile MongoClient mongoClient;
    private final String databaseName;
    private final String username;
    private final String password;
    private final String host;
    private final String authDb;

    /**
     * Creates a new data connection based on given config.
     */
    public MongoDataConnection(DataConnectionConfig config) {
        super(config);
        this.databaseName = config.getProperty(DATABASE_PROPERTY);
        this.username = config.getProperty(USERNAME_PROPERTY);
        this.password = config.getProperty(PASSWORD_PROPERTY);
        this.host = config.getProperty(HOST_PROPERTY);
        this.authDb = config.getProperty(AUTH_DB_PROPERTY, "admin");

        checkState(allSame((username == null), (password == null), (host == null)),
        "You have to provide connectionString property or combination of username, password and host");

        if (config.isShared()) {
            this.mongoClient = new CloseableMongoClient(createClient(config), this::release);
        }
    }

    private static boolean allSame(boolean... booleans) {
        if (booleans.length == 0) {
            return true;
        }
        boolean first = booleans[0];
        for (boolean aBoolean : booleans) {
            if (first != aBoolean) {
                return false;
            }
        }
        return true;
    }

    private MongoClient createClient(DataConnectionConfig config) {
        String connectionString = config.getProperty(CONNECTION_STRING_PROPERTY);
        if (connectionString != null) {
            return MongoClients.create(connectionString);
        }
        ServerAddress serverAddress = new ServerAddress(host);
        MongoCredential credential = MongoCredential.createCredential(username, authDb, password.toCharArray());
        Builder builder = MongoClientSettings.builder()
                                             .codecRegistry(defaultCodecRegistry())
                                             .applyToClusterSettings(s -> s.hosts(singletonList(serverAddress)))
                                             .credential(credential);
        return MongoClients.create(builder.build());
    }

    /**
     * Returns an instance of {@link MongoClient}.
     *
     * If client is {@linkplain DataConnectionConfig#isShared()} and there will be still some usages of given client,
     * the {@linkplain MongoClient#close()} method won't take an effect.
     */
    @Nonnull
    public MongoClient getClient() {
        if (getConfig().isShared()) {
            retain();
            checkState(mongoClient != null, "Mongo client should not be closed at this point");
            return mongoClient;
        } else {
            MongoClient client = createClient(getConfig());
            return new CloseableMongoClient(client, client::close);
        }
    }

    /**
     * Returns the database name hint.
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Lists all MongoDB collections in all databases.
     */
    @Nonnull
    @Override
    public List<DataConnectionResource> listResources() {
        List<DataConnectionResource> resources = new ArrayList<>();
        try (MongoClient client = getClient()) {
            if (databaseName != null) {
                MongoDatabase mongoDatabase = client.getDatabase(databaseName);
                addResources(resources, mongoDatabase);
            } else {
                for (String databaseName : client.listDatabaseNames()) {
                    MongoDatabase database = client.getDatabase(databaseName);
                    addResources(resources, database);
                }
            }
        }
        return resources;
    }

    private static void addResources(List<DataConnectionResource> resources, MongoDatabase database) {
        for (String collectionName : database.listCollectionNames()) {
            resources.add(new DataConnectionResource("collection", database.getName() + "." + collectionName));
        }
    }

    /**
     * Closes underlying client.
     */
    @Override
    public void destroy() {
        if (mongoClient != null) {
            ((CloseableMongoClient) mongoClient).unwrap().close();
            mongoClient = null;
        }
    }

    /**
     * Helper method to create new {@link MongoDataConnection} with given name and connection string.
     */
    @Nonnull
    public static DataConnectionConfig mongoDataConnectionConf(String name, String connectionString) {
        DataConnectionConfig dataConnectionConfig = new DataConnectionConfig();
        dataConnectionConfig.setName(name);
        dataConnectionConfig.setShared(true);
        dataConnectionConfig.setProperty(CONNECTION_STRING_PROPERTY, connectionString);
        dataConnectionConfig.setType("MongoDB");
        return dataConnectionConfig;
    }
}
