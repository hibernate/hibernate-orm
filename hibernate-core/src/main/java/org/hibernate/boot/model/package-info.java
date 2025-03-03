/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * This package defines the boot-time metamodel, which is an interpretation
 * of the domain model (entity classes, embeddable classes, and attributes)
 * and the mapping of these "domain model parts" to the database. It is
 * {@linkplain org.hibernate.boot.model.process.spi built incrementally} from
 * {@linkplain org.hibernate.annotations annotations} and XML-based mappings.
 * <p>
 * The interfaces {@link org.hibernate.boot.model.TypeContributor} and
 * {@link org.hibernate.boot.model.FunctionContributor} allow a program
 * or library to contribute custom types and type descriptors, and
 * custom function descriptors, respectively, to Hibernate during the
 * bootstrap process.
 *
 * @implNote Ultimately, as part of the process of creating the
 *           {@link org.hibernate.SessionFactory}, Hibernate
 *           transforms this boot-time metamodel to its runtime
 *           {@linkplain org.hibernate.metamodel.mapping mapping metamodel}.
 */
package org.hibernate.boot.model;
