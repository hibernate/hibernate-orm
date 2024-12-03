/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement;

import org.hibernate.bytecode.enhance.spi.UnsupportedEnhancementStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Steve Ebersole
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Inherited
public @interface EnhancementOptions {
	boolean biDirectionalAssociationManagement() default false;
	boolean inlineDirtyChecking() default false;
	boolean lazyLoading() default false;
	boolean extendedEnhancement() default false;
	UnsupportedEnhancementStrategy unsupportedEnhancementStrategy() default UnsupportedEnhancementStrategy.SKIP;
}
