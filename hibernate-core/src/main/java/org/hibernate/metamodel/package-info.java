/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * This package defines an API for accessing the runtime metamodel describing persistent
 * entities in Java and their mappings to the relational database schema.
 * <p>
 * The runtime metamodel may be divided into two layers:
 * <ul>
 * <li>The {@linkplain org.hibernate.metamodel.model.domain domain metamodel}, which is
 *     an extension of the {@linkplain jakarta.persistence.metamodel JPA metamodel} and
 *     is used in {@linkplain org.hibernate.query query} handling. This layer contains
 *     information about Java classes, but not about their mappings to the database.
 * <li>The {@linkplain org.hibernate.metamodel.mapping mapping metamodel}, which describes
 *     the application domain model and its mapping to the database. It is distinct from
 *     the JPA metamodel, which does not contain this O/R mapping information.
 * </ul>
 * @author Steve Ebersole
 */
@Incubating
package org.hibernate.metamodel;

import org.hibernate.Incubating;
