/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.model;

import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.mapping.Selectable;

/**
 * An implementation of {@link Selection} that represents a formula.
 *
 * @author Chris Cranford
 */
public class Formula extends Selection<String> {

	private final String formula;

	public Formula(String formula) {
		super( SelectionType.FORMULA );
		this.formula = formula;
	}

	@Override
	public String build() {
		return formula;
	}

	/**
	 * Create an Envers Formula mapping from an Hibernate ORM formula.
	 *
	 * @param formula the ORM formula
	 * @return the envers formula mapping
	 */
	public static Formula from(Selectable formula) {
		if ( !formula.isFormula() ) {
			throw new EnversMappingException( "Cannot create audit formula mapping from " + formula.getClass().getName() );
		}
		return new Formula( formula.getText() );
	}
}
