/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark test classes or methods as expecting to fail when using the new metamodel introduced in 5.0.  This annotation is
 * only honored when the system property 'hibernate.test.new_metadata_mappings' is set to <code>true</code>.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.METHOD, ElementType.TYPE } )
@FailureExpectedMarker
public @interface FailureExpectedWithNewMetamodel {

	/**
	 * @return an optional JIRA issue key that covers the expected failure
	 */
	String jiraKey() default "";

	/**
	 * @return an optional message related to the expected failure
	 */
	String message() default "";
}
