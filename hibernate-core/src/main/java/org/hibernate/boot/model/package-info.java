/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Package defining Hibernate's boot-time metamodel, which is an
 * understanding of the application's domain model (its entities,
 * attributes, etc.) and the mapping of those "domain model parts"
 * to the database.
 * <p/>
 * It is {@linkplain org.hibernate.boot.model.process incrementally built}
 * from {@linkplain org.hibernate.annotations annotations} and XML mappings
 *
 * @implNote Ultimately, as part of the process of creating the
 * {@link org.hibernate.SessionFactory}, Hibernate will interpret
 * this boot metamodel to its runtime
 * {@linkplain org.hibernate.metamodel.mapping mapping metamodel}
 */
package org.hibernate.boot.model;
