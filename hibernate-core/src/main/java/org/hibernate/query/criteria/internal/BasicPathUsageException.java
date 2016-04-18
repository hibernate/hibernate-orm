/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;
import javax.persistence.metamodel.Attribute;

/**
 * Represents an incorrect usage of a basic path.  Generally this means an attempt to
 * de-reference a basic attribute path.
 *
 * @author Steve Ebersole
 */
public class BasicPathUsageException extends RuntimeException {
	private final Attribute<?,?> attribute;

	/**
	 * Construct the usage exception.
	 *
	 * @param message An error message describing the incorrect usage.
	 * @param attribute The basic attribute involved.
	 */
	public BasicPathUsageException(String message, Attribute<?,?> attribute) {
		super( message );
		this.attribute = attribute;
	}

	/**
	 * Construct the usage exception.
	 *
	 * @param message An error message describing the incorrect usage.
	 * @param cause An underlying cause.
	 * @param attribute The basic attribute involved.
	 */
	public BasicPathUsageException(String message, Throwable cause, Attribute<?,?> attribute) {
		super( message, cause );
		this.attribute = attribute;
	}

	public Attribute<?,?> getAttribute() {
		return attribute;
	}
}
