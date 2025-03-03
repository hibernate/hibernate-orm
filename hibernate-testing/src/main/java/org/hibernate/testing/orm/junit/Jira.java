/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the URL to the Jira issue associated with a test.
 * Is repeatable, so multiple JIRA issues can be indicated.
 *
 * @see JiraGroup
 *
 * @author Steve Ebersole
 */
@Retention( RetentionPolicy.RUNTIME )
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable( JiraGroup.class  )
public @interface Jira {
	/**
	 * The URL to the Jira issue
	 */
	String value();
}
