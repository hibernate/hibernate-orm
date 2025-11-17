/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * This package defines the Hibernate configuration-time mapping model.
 * The objects defined in this package are produced by the annotation
 * binding process, and consumed by the code with builds persisters and
 * loaders. They do not outlive the configuration process.
 * <p>
 * The mapping model objects represent:
 * <ul>
 * <li>Java elements with a persistent representation, for example,
 *     a {@link org.hibernate.mapping.PersistentClass},
 *     {@link org.hibernate.mapping.Collection}, or
 *     {@link org.hibernate.mapping.Property}, and
 * <li>objects in a relational database, for example,
 *     a {@link org.hibernate.mapping.Table},
 *     {@link org.hibernate.mapping.Column}, or
 *     {@link org.hibernate.mapping.ForeignKey}.
 * </ul>
 * <p>
 * The lifecycle of these mapping objects is outlined below.
 * <ol>
 * <li>It is the responsibility of the metadata binders in the package
 *     {@link org.hibernate.boot.model.internal} to process a set of
 *     annotated classes and produce fully-initialized mapping model
 *     objects. This is in itself a complicated multiphase process,
 *     since, for example, the type of an association mapping in one
 *     entity cannot be fully assigned until the target entity has
 *     been processed.
 * <li>The mapping model objects are then passed to the constructor
 *     of {@link org.hibernate.internal.SessionFactoryImpl}, which
 *     simply passes them along on to an object which implements
 *     {@link org.hibernate.metamodel.MappingMetamodel} and uses them
 *     to create persister objects for
 *     {@linkplain org.hibernate.persister.entity.EntityPersister entities}
 *     and {@linkplain org.hibernate.persister.collection.CollectionPersister collections}.
 * <li>The model objects are used directly in the constructors of
 *     {@link org.hibernate.tuple.entity.EntityMetamodel},
 *     {@link org.hibernate.persister.entity.AbstractEntityPersister},
 *     {@link org.hibernate.persister.collection.AbstractCollectionPersister},
 *     and friends, to build up the internal data structures used by these
 *     objects at runtime. Once the persisters are fully-constructed,
 *     the mapping model objects are no longer useful.
 * <li>The mapping model objects are also passed to the schema
 *     export tooling which uses them directly to produce DDL.
 * </ol>
 *
 * @see org.hibernate.boot
 */
package org.hibernate.mapping;
