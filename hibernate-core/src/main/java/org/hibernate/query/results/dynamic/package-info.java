/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Support for mutable result/fetch builder graphs nodes built dynamically via
 * Hibernate's {@link org.hibernate.NativeQuery} APIs
 *
 * @see org.hibernate.NativeQuery#addScalar
 * @see org.hibernate.NativeQuery#addEntity
 * @see org.hibernate.NativeQuery#addJoin
 * @see org.hibernate.NativeQuery#addFetch
 * @see org.hibernate.NativeQuery#addRoot
 */
package org.hibernate.query.results.dynamic;
