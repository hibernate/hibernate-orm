/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation used to indicate that a test should be run only when the current dialect supports the
 * specified feature.
 *
 * @author Andrea Boriero
 */
@Inherited
@Retention( RetentionPolicy.RUNTIME )
@Target({ ElementType.TYPE, ElementType.METHOD})
@Repeatable( RequiresDialectFeatureGroup.class  )

@ExtendWith( DialectFilterExtension.class )
public @interface RequiresDialectFeature {
	/**
	 * @return Class which checks the necessary dialect feature
	 */
	Class<? extends DialectFeatureCheck> feature();

	/**
	 * @return Whether the decision of {@link #feature()} is reversed
	 */
	boolean reverse() default false;

	/**
	 * Comment describing the reason why the feature is required.
	 *
	 * @return The comment
	 */
	String comment() default "";

	/**
	 * The key of a JIRA issue which relates this this feature requirement.
	 *
	 * @return The jira issue key
	 */
	String jiraKey() default "";
}
