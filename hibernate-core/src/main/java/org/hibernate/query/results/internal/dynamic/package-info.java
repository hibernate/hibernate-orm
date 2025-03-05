/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
