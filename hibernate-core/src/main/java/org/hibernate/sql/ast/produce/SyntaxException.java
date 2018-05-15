/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce;

/**
 * Represents a problem in the SQM in terms of improper syntax used.
 * <p/>
 * This is distinct from problems in the converter itself (Hibernate bug)
 * which is reported as {@link ConversionException}.
 *
 * @author Steve Ebersole
 */
public class SyntaxException extends SqlTreeException {
	public SyntaxException(String message) {
		super( message );
	}
}
