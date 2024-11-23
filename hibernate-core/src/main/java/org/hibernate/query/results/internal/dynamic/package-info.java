/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */

/**
 * Support for mutable result/fetch builder graphs nodes built dynamically.
 * Using, for example, Hibernate's {@link org.hibernate.query.NativeQuery} API.
 *
 * @see org.hibernate.query.NativeQuery#addScalar
 * @see org.hibernate.query.NativeQuery#addEntity
 * @see org.hibernate.query.NativeQuery#addJoin
 * @see org.hibernate.query.NativeQuery#addFetch
 * @see org.hibernate.query.NativeQuery#addRoot
 */
package org.hibernate.query.results.internal.dynamic;
