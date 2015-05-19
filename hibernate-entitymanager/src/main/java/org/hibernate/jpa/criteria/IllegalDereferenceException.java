/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria;


/**
 * Represents an illegal attempt to dereference from a {@link #getPathSource() path source} which
 * cannot be dereferenced.
 *
 * @author Steve Ebersole
 */
public class IllegalDereferenceException extends RuntimeException {
	private final PathSource pathSource;

	public IllegalDereferenceException(PathSource pathSource) {
		super( "Illegal attempt to dereference path source [" + pathSource.getPathIdentifier() + "]" );
		this.pathSource = pathSource;
	}

	public PathSource getPathSource() {
		return pathSource;
	}
}
