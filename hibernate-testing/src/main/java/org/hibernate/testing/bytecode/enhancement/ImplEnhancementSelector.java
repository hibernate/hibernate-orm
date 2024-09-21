/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Steve Ebersole
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( ElementType.TYPE)
@Inherited
@Repeatable( ImplEnhancementSelectors.class )
public @interface ImplEnhancementSelector {
	Class<? extends EnhancementSelector> impl();
}
