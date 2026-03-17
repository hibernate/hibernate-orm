/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.scan.jandex;


import org.hibernate.boot.scan.Discoverable;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;

/// Mimic [jakarta.persistence.Entity] with [Discoverable]
///
/// @author Steve Ebersole
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Discoverable
public @interface Entity {
}
