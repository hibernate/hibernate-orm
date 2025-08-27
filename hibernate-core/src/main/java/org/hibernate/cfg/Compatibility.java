/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;

/**
 * Denotes that a setting is intended to allow applications to upgrade
 * versions of Hibernate and maintain backwards compatibility with the
 * older version in some specific behavior. Such settings are almost always
 * considered temporary and are usually also {@linkplain Deprecated deprecated}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Compatibility {
}
