/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A grouping of {@link NamedNativeQuery} definitions. Extends the named native query
 * definitions made available through {@link jakarta.persistence.NamedNativeQueries}.
 *
 * @author Emmanuel Bernard
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
public @interface NamedNativeQueries {
	/**
	 * The grouping of Hibernate named native SQL queries.
	 */
	NamedNativeQuery[] value();
}
