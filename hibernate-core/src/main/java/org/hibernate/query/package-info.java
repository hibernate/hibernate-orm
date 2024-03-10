/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Everything related to HQL/JPQL, native SQL, and criteria queries.
 * <p>
 * The important interfaces {@link org.hibernate.query.SelectionQuery},
 * {@link org.hibernate.query.MutationQuery}, {@link org.hibernate.query.Query},
 * and {@link org.hibernate.query.NativeQuery} provide an API for executing
 * queries. Instances of these interfaces may be obtained from a
 * {@link org.hibernate.query.QueryProducer}, that is, from any
 * {@link org.hibernate.Session} or {@link org.hibernate.StatelessSession}.
 * <p>
 * The classes {@link org.hibernate.query.Order}, {@link org.hibernate.query.Page},
 * {@link org.hibernate.query.KeyedPage}, and {@link org.hibernate.query.KeyedResultList}
 * define an API for dynamic ordering and key-based pagination. These
 * classes are especially useful as parameters of generated
 * {@link org.hibernate.annotations.processing.Find @Find} and
 * {@link org.hibernate.annotations.processing.HQL @HQL} methods.
 * <p>
 * Hibernate's extensions to the JPA criteria query API are defined in the
 * subpackage {@link org.hibernate.query.criteria}, with
 * {@link org.hibernate.query.criteria.HibernateCriteriaBuilder} as the
 * entry point.
 * <p>
 * Other subpackages contain SPIs and internal implementation details,
 * including the HQL parser and translator.
 */
package org.hibernate.query;
