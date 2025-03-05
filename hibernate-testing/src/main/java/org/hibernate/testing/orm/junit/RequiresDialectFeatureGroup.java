/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation used to indicate that a test should be run only when the current dialect supports the
 * specified feature.
 *
 * @author Hardy Ferentschik
 */
@Inherited
@Retention( RetentionPolicy.RUNTIME )
@Target({ ElementType.TYPE, ElementType.METHOD})

@ExtendWith( DialectFilterExtension.class )
public @interface RequiresDialectFeatureGroup {
	RequiresDialectFeature[]  value();

	/**
	 * The key of a JIRA issue which relates this this feature requirement.
	 *
	 * @return The jira issue key
	 */
	String jiraKey() default "";
}
