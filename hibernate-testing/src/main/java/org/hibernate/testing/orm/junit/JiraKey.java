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
 * Identifies the JIRA issue associated with a test.  Is repeatable, so
 * multiple JIRA issues can be indicated.
 *
 * @see JiraKeyGroup
 *
 * @author Steve Ebersole
 */
@Retention( RetentionPolicy.RUNTIME )
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable( JiraKeyGroup.class  )
public @interface JiraKey {
	/**
	 * The key for the referenced Jira issue (e.g., HHH-99999)
	 */
	String value();
}
