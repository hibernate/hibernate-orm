/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * This package defines APIs for configuring Hibernate.
 * <ul>
 * <li>{@link org.hibernate.cfg.AvailableSettings} enumerates all the
 *     configuration properties recognized by Hibernate.
 * <li>{@link org.hibernate.cfg.Configuration} provides a simplified
 *     API for bootstrapping Hibernate, as an alternative to the more
 *     extensive facilities defined under {@link org.hibernate.boot}.
 * </ul>
 * <p>
 * Note that all the real meat behind these APIs is defined in the package
 * {@link org.hibernate.boot}.
 * <p>
 * Configuration properties may be specified:
 * <ul>
 * <li>in Java code that {@linkplain org.hibernate.cfg.Configuration#setProperty
 *     configures} Hibernate,
 * <li>in a JPA configuration file named {@code persistence.xml},
 * <li>in a native configuration file named {@code hibernate.cfg.xml},
 * <li>in a file named {@code hibernate.properties}, or
 * <li>using some container-specific configuration facility, for example,
 *     Quarkus configuration properties.
 * </ul>
 * <p>
 * We now present a couple of example configuration files.
 *
 * <h3>Example JPA configuration file</h3>
 *
 * The following JPA configuration may be specified in a file named {@code persistence.xml}:
 *
 * <pre>{@code <persistence xmlns="http://java.sun.com/xml/ns/persistence"
 *              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *              xsi:schemaLocation="http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd"
 *              version="2.0">
 *
 *     <persistence-unit name="postgresql-example" transaction-type="RESOURCE_LOCAL">
 *
 *         <class>org.hibernate.orm.example.Author</class>
 *         <class>org.hibernate.orm.example.Book</class>
 *
 *         <properties>
 *
 *             <!-- PostgreSQL -->
 *             <property name="javax.persistence.jdbc.url"
 *                       value="jdbc:postgresql://localhost/library"/>
 *
 *             <!-- Credentials -->
 *             <property name="javax.persistence.jdbc.user"
 *                       value="hibernate"/>
 *             <property name="javax.persistence.jdbc.password"
 *                       value="hibernate"/>
 *
 *             <!-- Agroal connection pool config -->
 *             <property name="hibernate.agroal.maxSize"
 *                       value="10"/>
 *             <property name="hibernate.agroal.acquisitionTimeout"
 *                       value="PT1s"/>
 *             <property name="hibernate.agroal.reapTimeout"
 *                       value="PT10s"/>
 *
 *             <!-- Automatic schema export -->
 *             <property name="javax.persistence.schema-generation.database.action"
 *                       value="drop-and-create"/>
 *
 *             <!-- SQL statement logging -->
 *             <property name="hibernate.show_sql" value="true"/>
 *             <property name="hibernate.format_sql" value="true"/>
 *             <property name="hibernate.highlight_sql" value="true"/>
 *
 *         </properties>
 *     </persistence-unit>
 *
 * </persistence>}</pre>
 *
 * The JPA configuration file is necessary when bootstrapping Hibernate via
 * {@link jakarta.persistence.Persistence#createEntityManagerFactory(java.lang.String)}.
 *
 * <h3>Example native configuration file</h3>
 *
 * The following configuration may be specified in a file named {@code hibernate.cfg.xml}:
 *
 * <pre>{@code <?xml version='1.0' encoding='utf-8'?>
 * <!DOCTYPE hibernate-configuration PUBLIC
 *         "-//Hibernate/Hibernate Configuration DTD 3.0//EN"
 *         "http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd">
 *
 * <hibernate-configuration>
 *     <session-factory>
 *         <!-- PostgreSQL -->
 *         <property name="javax.persistence.jdbc.url">jdbc:postgresql://localhost/library</property>
 *
 *         <!-- Credentials -->
 *         <property name="hibernate.connection.username">hibernate</property>
 *         <property name="hibernate.connection.password">hibernate</property>
 *
 *         <!-- Agroal connection pool config -->
 *         <property name="hibernate.agroal.maxSize">10</property>
 *         <property name="hibernate.agroal.acquisitionTimeout">PT1s</property>
 *         <property name="hibernate.agroal.reapTimeout">PT10s</property>
 *
 *         <!-- Automatic schema export -->
 *         <property name="hibernate.hbm2ddl.auto">create</property>
 *
 *         <!-- SQL statement logging -->
 *         <property name="hibernate.show_sql">true</property>
 *         <property name="hibernate.format_sql">true</property>
 *         <property name="hibernate.highlight_sql">true</property>
 *
 *         <!-- Maximum JDBC batch size -->
 *         <property name="hibernate.jdbc.batch_size">10</property>
 *
 *         <!-- Entity classes -->
 *         <mapping class="org.hibernate.orm.example.Author"/>
 *         <mapping class="org.hibernate.orm.example.Book"/>
 *
 *     </session-factory>
 * </hibernate-configuration>}</pre>
 *
 * The native configuration file is used when configuring Hibernate via
 * {@link org.hibernate.cfg.Configuration#configure()} or
 * {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder#configure()}.
 */
package org.hibernate.cfg;
