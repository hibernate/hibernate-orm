/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

/**
 * Indicates a problem with the labelling of query parameters.
 *
 * @author Gavin King
 *
 * @since 6.3
 */
public class ParameterLabelException extends SemanticException {
	public ParameterLabelException(String message) {
		super(message);
	}
}
