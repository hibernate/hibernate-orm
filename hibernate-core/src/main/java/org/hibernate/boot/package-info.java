/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * This package contains the interfaces that make up the bootstrap API
 * for Hibernate. They collectively provide a way to specify configuration
 * information and construct a new instance of {@link org.hibernate.SessionFactory}.
 * <p>
 * Configuring Hibernate using these APIs usually involves working with:
 * <ol>
 * <li>{@link org.hibernate.boot.registry.StandardServiceRegistryBuilder},
 *     then
 * <li>{@link org.hibernate.boot.MetadataSources} and
 *     {@link org.hibernate.boot.MetadataBuilder}, and
 * <li>finally, with {@link org.hibernate.boot.SessionFactoryBuilder}.
 * </ol>
 * <pre>
 * StandardServiceRegistry standardRegistry =
 *         new StandardServiceRegistryBuilder()
 *                 // supply a configuration
 *                 .configure("org/hibernate/example/hibernate.cfg.xml")
 *                 // set a configuration property
 *                 .applySetting(AvailableSettings.HBM2DDL_AUTO,
 *                               SchemaAutoTooling.CREATE_DROP.externalForm())
 *                 .build();
 * MetadataBuilder metadataBuilder =
 *         new MetadataSources(standardRegistry)
 *                 // supply annotated classes
 *                 .addAnnotatedClass(MyEntity.class)
 *                 .addAnnotatedClassName("org.hibernate.example.Customer")
 *                 // supply XML-based mappings
 *                 .addResource("org/hibernate/example/Order.hbm.xml")
 *                 .addResource("org/hibernate/example/Product.orm.xml")
 *                 .getMetadataBuilder();
 * Metadata metadata =
 *         metadataBuilder
 *                 // set the naming strategies
 *                 .applyImplicitNamingStrategy(ImplicitNamingStrategyJpaCompliantImpl.INSTANCE)
 *                 .applyPhysicalNamingStrategy(new CustomPhysicalNamingStrategy())
 *                 // add a TypeContributor
 *                 .applyTypes(new CustomTypeContributor())
 *                 .build();
 * SessionFactoryBuilder sessionFactoryBuilder =
 *         metadata.getSessionFactoryBuilder();
 * SessionFactory sessionFactory =
 *         sessionFactoryBuilder
 *                 // supply a factory-level Interceptor
 *                 .applyInterceptor(new CustomSessionFactoryInterceptor());
 *                 // add a custom observer
 *                 .addSessionFactoryObservers(new CustomSessionFactoryObserver());
 *                 // apply a CDI BeanManager (for JPA event listeners)
 *                 .applyBeanManager(getBeanManager());
 *                 .build();
 * </pre>
 * <p>
 * In more advanced scenarios,
 * {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder}
 * might also be used.
 * <p>
 * See the <em>Native Bootstrapping</em> guide for more details.
 * <p>
 * Included in subpackages under this namespace are:
 * <ul>
 * <li>{@linkplain org.hibernate.boot.registry implementations} of
 *     {@link org.hibernate.service.ServiceRegistry} used during
 *     the bootstrap process,
 * <li>implementations of {@link org.hibernate.boot.MetadataBuilder}
 *     and {@link org.hibernate.boot.Metadata}, and
 *     {@link org.hibernate.boot.SessionFactoryBuilder},
 * <li>{@linkplain org.hibernate.boot.model.naming support} for
 *     {@link org.hibernate.boot.model.naming.ImplicitNamingStrategy}
 *     and {@link org.hibernate.boot.model.naming.PhysicalNamingStrategy},
 * <li>{@linkplain org.hibernate.boot.spi a range of SPIs} allowing
 *     integration with the process of building metadata,
 * <li>internal code for parsing and interpreting mapping information
 *     declared in XML or using annotations,
 * <li>{@linkplain org.hibernate.boot.beanvalidation support} for
 *     integrating an implementation of Bean Validation, such as
 *     <a href="https://hibernate.org/validator/">Hibernate Validator</a>,
 *     and
 * <li>{@linkplain org.hibernate.boot.model.relational some SPIs}
 *     used for schema management, including support for exporting
 *     {@linkplain org.hibernate.boot.model.relational.AuxiliaryDatabaseObject
 *     auxiliary database objects}, and for determining the
 *     {@linkplain org.hibernate.boot.model.relational.ColumnOrderingStrategy
 *     order of columns} in generated DDL statements.
 * </ul>
 */
package org.hibernate.boot;
