/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * This package defines the central Hibernate APIs, beginning with
 * {@link org.hibernate.SessionFactory}, which represents an instance of
 * Hibernate at runtime and is the source of new instances of
 * {@link org.hibernate.Session} and {@link org.hibernate.StatelessSession},
 * the most important APIs exposing persistence-related operations for
 * entities. The interface {@link org.hibernate.SharedSessionContract}
 * declares operations that are common to both stateful and stateless sessions.
 * <p>
 * Playing important supporting roles here, we also have
 * {@link org.hibernate.Transaction}, {@link org.hibernate.Filter},
 * and {@link org.hibernate.Cache}.
 * <p>
 * APIs related to querying are now defined under the namespace
 * {@link org.hibernate.query}.
 */
package org.hibernate;
