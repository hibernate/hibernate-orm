/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Steve Ebersole
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( ElementType.TYPE)
@Inherited
public @interface ClassEnhancementSelectors {
	ClassEnhancementSelector[] value();
}
