/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * An API for user-defined custom types which extend the set of built-in
 * {@linkplain org.hibernate.type.Type types} defined in {@link org.hibernate.type}.
 * <p>
 * A custom type might map a {@linkplain org.hibernate.usertype.UserType single column},
 * or it might map {@linkplain org.hibernate.usertype.CompositeUserType multiple columns}.
 *
 * @see org.hibernate.usertype.UserType
 * @see org.hibernate.usertype.CompositeUserType
 * @see org.hibernate.type
 *
 * @apiNote Historically, {@link org.hibernate.usertype.UserType} was the
 *          most important extension point in Hibernate, and
 *          {@link org.hibernate.usertype.CompositeUserType} was much less
 *          popular. But in modern Hibernate, the terrain formerly occupied
 *          by {@code UserType} has been encroached, first by
 *          {@link jakarta.persistence.AttributeConverter}, and then by the
 *          new {@linkplain org.hibernate.type "compositional" approach to
 *          basic types}. Contrariwise, {@code CompositeUserType} has been
 *          redesigned and is now more powerful and much easier to use.
 */
package org.hibernate.usertype;
