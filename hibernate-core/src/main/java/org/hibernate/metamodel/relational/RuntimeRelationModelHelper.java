/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.relational;

/**
 * @author Steve Ebersole
 */
public class RuntimeRelationModelHelper {
	public static final String DEFAULT_COLUMN_WRITE_EXPRESSION = "?";

	private RuntimeRelationModelHelper() {
		// disallow direct instantiation
	}
}
