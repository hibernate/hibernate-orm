/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import org.hibernate.HibernateException;

/**
 * Exception indicating that a formula mapping was encountered where it is not currently supported
 *
 * @author Steve Ebersole
 */
public class FormulaNotSupportedException extends HibernateException {
	private static final String MSG = "Formula mappings (aside from @DiscriminatorValue) are currently not supported";

	/**
	 * Constructs a FormulaNotSupportedException using a standard message
	 */
	public FormulaNotSupportedException() {
		super( MSG );
	}
}
