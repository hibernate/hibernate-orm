/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Defines the runtime domain metamodel, which describes the Java aspect of
 * the application's domain model parts (entities, attributes).
 * <p>
 * The API defined here extends and implements the standard
 * {@linkplain jakarta.persistence.metamodel JPA metamodel}.
 * <p>
 * This metamodel is used in {@linkplain org.hibernate.query query} handling.
 *
 * @see jakarta.persistence.metamodel
 */
@Incubating
package org.hibernate.metamodel.model.domain;

import org.hibernate.Incubating;
