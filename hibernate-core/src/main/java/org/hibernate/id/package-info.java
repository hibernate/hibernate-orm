/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * This package and its subpackages, especially {@link org.hibernate.id.enhanced},
 * contain the built-in id generators, all of which implement either
 * {@link org.hibernate.id.IdentifierGenerator} or
 * {@link org.hibernate.id.PostInsertIdentifierGenerator}.
 * <p>
 * The most useful id generators in modern Hibernate are:
 * <ul>
 *     <li>{@link org.hibernate.id.IdentityGenerator} - {@code @GeneratedValue(strategy=IDENTITY)}
 *     <li>{@link org.hibernate.id.enhanced.SequenceStyleGenerator} - {@code @GeneratedValue(strategy=SEQUENCE)}
 *     <li>{@link org.hibernate.id.enhanced.TableGenerator} - {@code @GeneratedValue(strategy=TABLE)}
 *     <li>{@link org.hibernate.id.uuid.UuidGenerator} - {@code @UuidGenerator}
 * </ul>
 * <p>
 * @apiNote The remaining id generators are kept around for backward compatibility
 *          and as an implementation detail of the {@code hbm.xml} mapping format.
 *
 * @see org.hibernate.generator
 */
package org.hibernate.id;
