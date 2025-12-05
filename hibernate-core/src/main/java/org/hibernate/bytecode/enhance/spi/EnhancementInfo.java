/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides basic information about the enhancement done to a class.
 * Used to verify attempts to re-enhance an already enhanced class.
 */
@Target( ElementType.TYPE )
@Retention( RetentionPolicy.RUNTIME )
public @interface EnhancementInfo {
	/**
	 * The Hibernate version used for enhancement.
	 */
	String version();

	/**
	 * Whether dirty checking was enabled when the targeted class was enhanced.
	 */
	boolean includesDirtyChecking();

	/**
	 * Whether bidirectional association management was enabled when the targeted class was enhanced.
	 */
	boolean includesAssociationManagement();
}
