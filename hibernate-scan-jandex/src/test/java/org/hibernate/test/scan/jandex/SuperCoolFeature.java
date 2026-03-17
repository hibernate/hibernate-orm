/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.scan.jandex;

import org.hibernate.boot.scan.Discoverable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// @author Steve Ebersole
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Discoverable
public @interface SuperCoolFeature {
}
