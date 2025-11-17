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
 * A grouping of {@link NamedQuery} definitions. Extends the named query definitions
 * made available through {@link jakarta.persistence.NamedQueries}.
 *
 * @author Emmanuel Bernard
 * @author Carlos Gonzalez-Cadenas
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
public @interface NamedQueries {
	/**
	 * The grouping of named queries.
	 */
	NamedQuery[] value();
}
