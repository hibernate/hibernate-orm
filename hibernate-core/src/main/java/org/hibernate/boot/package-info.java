/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/// This package contains the interfaces that make up the bootstrap API for
/// Hibernate.  They collectively provide a way to specify configuration
/// information and construct a new instance of [org.hibernate.SessionFactory].
///
/// Native programmatic bootstrap is exposed via
/// [org.hibernate.jpa.HibernatePersistenceConfiguration], which extends Jakarta
/// Persistence's programmatic bootstrap API with Hibernate-specific conveniences.
///
/// ```java
/// SessionFactory sessionFactory =
///         new org.hibernate.jpa.HibernatePersistenceConfiguration("example")
///                 .property(AvailableSettings.HBM2DDL_AUTO, "create-drop")
///                 .managedClass(MyEntity.class)
///                 .createEntityManagerFactory();
/// ```
///
/// In more advanced scenarios,
/// [org.hibernate.boot.registry.BootstrapServiceRegistryBuilder] might also be
/// used.
///
/// ## Metadata serialization
///
/// Resolved boot metadata may be converted to a data-only, factory-ready
/// [org.hibernate.boot.serial.MetadataArchive] and restored later in another
/// bootstrap environment. Producers opt in before building metadata using
/// [org.hibernate.cfg.MappingSettings#METADATA_SERIALIZATION_ENABLED]. The
/// archive includes declarative metadata, recipes
/// for rebuilding derived basic-value resolutions, and the runtime metamodel
/// handoff needed by SessionFactory construction. Reading the archive
/// is separate from restoring the live metadata graph, so application classes
/// are not loaded until the consumer explicitly supplies its service registry:
///
/// ```java
/// MetadataArchive archive;
/// try (InputStream input = Files.newInputStream(metadataFile)) {
///     archive = MetadataSerialization.read(input);
/// }
/// RestoredMetadata restored = archive.restore(serviceRegistry);
/// Metadata metadata = restored.getMetadata();
/// SessionFactory sessionFactory = restored.buildSessionFactory();
/// ```

/// Restoration validates the type-affecting environment and completes mapping
/// resolution before returning. A bare, inspection-only metadata graph is not a
/// supported archive product.
/// Archives are trusted build artifacts and are accepted only from the exact
/// same Hibernate ORM version and archive format; they must not be read from an
/// untrusted source.
/// Resolution details are retained only for an opted-in producer and are
/// converted to immutable restoration recipes when serialization is requested.
///
/// See [org.hibernate.boot.serial.MetadataSerialization] for producer and consumer examples.
///
/// See the *Native Bootstrapping* guide for more details.
///
/// Included in subpackages under this namespace are:
///
///   - [implementations][org.hibernate.boot.registry] of
///     [org.hibernate.service.ServiceRegistry] used during the bootstrap process,
///   - implementations of [org.hibernate.boot.Metadata],
///   - [support][org.hibernate.boot.model.naming] for
///     [org.hibernate.boot.model.naming.ImplicitNamingStrategy] and
///     [org.hibernate.boot.model.naming.PhysicalNamingStrategy],
///   - [a range of SPIs][org.hibernate.boot.spi] allowing integration with the
///     process of building metadata,
///   - internal code for parsing and interpreting mapping information declared
///     in XML or using annotations,
///   - [support][org.hibernate.boot.beanvalidation] for integrating an
///     implementation of Bean Validation, such as
///     [Hibernate Validator](https://hibernate.org/validator/), and
///   - [some SPIs][org.hibernate.boot.model.relational] used for schema
///     management, including support for exporting
///     [auxiliary database objects][org.hibernate.boot.model.relational.AuxiliaryDatabaseObject],
///     and for determining the
///     [order of columns][org.hibernate.boot.model.relational.ColumnOrderingStrategy]
///     in generated DDL statements.
package org.hibernate.boot;
