/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Integrates a range of types defined by the JDK with the type system
 * of Hibernate. Each Java type is described by an implementation of
 * {@link org.hibernate.type.descriptor.java.JavaType}.
 * <p>
 * Certain important aspects related to the mutability or immutability
 * of a Java type are described by an associated
 * {@link org.hibernate.type.descriptor.java.MutabilityPlan}. In particular,
 * the right {@code MutabilityPlan} allows for correct dirty-checking and
 * destructured storage of values in the second-level cache.
 * <p>
 * See {@linkplain org.hibernate.type this discussion} of the roles
 * {@code JavaType} and {@code MutabilityPlan} play in basic type mappings.
 *
 * @see org.hibernate.type
 *
 * @see org.hibernate.type.descriptor.java.JavaType
 * @see org.hibernate.type.descriptor.java.MutabilityPlan
 */
package org.hibernate.type.descriptor.java;
