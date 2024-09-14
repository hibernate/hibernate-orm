/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm;

/**
 * Indicates an attempt to dereference a terminal path
 * (usually a path referring to something of basic type)
 *
 * @apiNote extends {@link IllegalStateException} to
 *          satisfy a questionable requirement of the JPA
 *          criteria query API
 *
 * @since 6.3
 *
 * @author Gavin King
 */
public class TerminalPathException extends IllegalStateException {
	public TerminalPathException(String message) {
		super(message);
	}
}
