/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.testing.orm.junit.SessionFactoryExtension;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Indicates that the annotated test class/method should
 * only be run when the current Dialect supports the function.
 *
 * @author Karel Maesen
 */

@Inherited
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)

@ExtendWith({ SessionFactoryExtension.class, RequiresFunctionExtension.class })
public @interface RequiresFunction {
	/**
	 * The key for the function (as used in the SqmFunctionRegistry)
	 */
	String key();

}
