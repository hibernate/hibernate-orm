/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used in mapping dynamic models to specify the java-type of an attribute, mainly for
 * {@linkplain jakarta.persistence.Basic basic},
 * {@linkplain jakarta.persistence.Id id},
 * {@linkplain jakarta.persistence.Embedded embedded} and
 * {@linkplain jakarta.persistence.EmbeddedId embedded-id} attributes.
 * Can also be useful for {@linkplain org.hibernate.annotations.Any any} and
 * {@linkplain org.hibernate.annotations.ManyToAny many-to-any} attributes to
 * specify a base type.
 * <p/>
 * Other attribute classifications have spec-defined ways to specify the target<ul>
 *     <li>{@linkplain jakarta.persistence.ManyToOne#targetEntity()}</li>
 *     <li>{@linkplain jakarta.persistence.OneToOne#targetEntity()}</li>
 *     <li>{@linkplain jakarta.persistence.ElementCollection#targetClass()} </li>
 *     <li>{@linkplain jakarta.persistence.OneToMany#targetEntity()}</li>
 *     <li>{@linkplain jakarta.persistence.ManyToMany#targetEntity()}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
@java.lang.annotation.Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Target {
	/**
	 * The attribute's Java type
	 */
	String value();
}
