/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2017-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.reveng.internal.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataConstants;
import org.hibernate.tool.reveng.api.reveng.RevengDialect;
import org.hibernate.tool.reveng.api.reveng.RevengDialectFactory;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.hibernate.tool.reveng.api.reveng.RevengStrategyFactory;
import org.hibernate.tool.reveng.internal.builder.db.DynamicEntityBuilder;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;
import org.hibernate.tool.reveng.internal.reader.ModelsDatabaseSchemaReader;

public class RevengMetadataDescriptor implements MetadataDescriptor {

    private final RevengStrategy reverseEngineeringStrategy;
    private final Properties properties = new Properties();
    private Metadata metadata;
    private MetadataBootstrapper.MetadataContext metadataContext;

    private List<ClassDetails> entityClassDetails;
    private ModelsContext modelsContext;
    private DynamicEntityBuilder entityBuilder;

    public RevengMetadataDescriptor(
            RevengStrategy reverseEngineeringStrategy,
            Properties properties) {
        this.properties.putAll(Environment.getProperties());
        if (properties != null) {
            this.properties.putAll(properties);
        }
        this.reverseEngineeringStrategy = Objects.requireNonNullElseGet(
                reverseEngineeringStrategy,
                RevengStrategyFactory::createReverseEngineeringStrategy);
        this.properties.putIfAbsent(
                MetadataConstants.PREFER_BASIC_COMPOSITE_IDS, true);
    }

    public Properties getProperties() {
        Properties result = new Properties();
        result.putAll(properties);
        return result;
    }

    public Metadata createMetadata() {
        if (metadata == null) {
            metadataContext = MetadataBootstrapper.bootstrap(
                    getEntityClassDetails(), properties);
            metadata = metadataContext.metadata();
        }
        return metadata;
    }

    public List<ClassDetails> getEntityClassDetails() {
        if (entityClassDetails == null) {
            buildEntityClassDetails();
        }
        return entityClassDetails;
    }

    public ModelsContext getModelsContext() {
        if (modelsContext == null) {
            buildEntityClassDetails();
        }
        return modelsContext;
    }

    private void buildEntityClassDetails() {
        StandardServiceRegistry serviceRegistry =
                new StandardServiceRegistryBuilder()
                        .applySettings(properties)
                        .build();
        try {
            Dialect dialect = serviceRegistry
                    .getService(JdbcServices.class).getDialect();
            ConnectionProvider connectionProvider = serviceRegistry
                    .getService(ConnectionProvider.class);
            RevengDialect revengDialect =
                    RevengDialectFactory.createMetaDataDialect(
                            dialect, properties);
            try {
                revengDialect.configure(connectionProvider);
                String defaultCatalog = (String) properties.get(
                        AvailableSettings.DEFAULT_CATALOG);
                String defaultSchema = (String) properties.get(
                        AvailableSettings.DEFAULT_SCHEMA);
                Object preferBasic = properties.get(
                        MetadataConstants.PREFER_BASIC_COMPOSITE_IDS);
                boolean preferBasicBool = !(preferBasic instanceof Boolean)
                        || (Boolean) preferBasic;
                List<TableDescriptor> tables =
                        ModelsDatabaseSchemaReader.create(
                                revengDialect,
                                reverseEngineeringStrategy,
                                defaultCatalog,
                                defaultSchema,
                                preferBasicBool)
                        .readSchema();
                DynamicEntityBuilder builder =
                        new DynamicEntityBuilder();
                builder.setPreferBasicCompositeIds(preferBasicBool);
                List<ClassDetails> entities = new ArrayList<>();
                for (TableDescriptor table : tables) {
                    entities.add(
                            builder.createEntityFromTable(table));
                }
                entities.addAll(builder.getEmbeddableClassDetails());
                this.entityClassDetails = entities;
                this.modelsContext = builder.getModelsContext();
                this.entityBuilder = builder;
            }
            finally {
                revengDialect.close();
            }
        }
        finally {
            StandardServiceRegistryBuilder.destroy(serviceRegistry);
        }
    }

    public Map<String, Map<String, List<String>>> getAllClassMetaAttributes() {
        if (entityBuilder == null) {
            buildEntityClassDetails();
        }
        return entityBuilder != null
                ? entityBuilder.getAllClassMetaAttributes()
                : Collections.emptyMap();
    }

    public Map<String, Map<String, Map<String, List<String>>>> getAllFieldMetaAttributes() {
        if (entityBuilder == null) {
            buildEntityClassDetails();
        }
        return entityBuilder != null
                ? entityBuilder.getAllFieldMetaAttributes()
                : Collections.emptyMap();
    }

}
