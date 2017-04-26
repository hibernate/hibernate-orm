/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

/**
 * Indicates a problem constructing a tree or a node within the tree.
 *
 * @author Steve Ebersole
 */
public class TreeException extends RuntimeException {
	public TreeException(String message) {
		super( message );
	}

	public TreeException(String message, Throwable cause) {
		super( message, cause );
	}
}
