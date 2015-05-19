/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ordering.antlr;

/**
 * Reference to a formula fragment.
 *
 * @author Steve Ebersole
 */
public interface FormulaReference extends SqlValueReference {
	/**
	 * Retrieve the formula fragment.  It is important to note that this is what the persister calls the
	 * "formula template", which has the $PlaceHolder$ (see {@link org.hibernate.sql.Template#TEMPLATE})
	 * markers injected.
	 *
	 * @return The formula fragment template.
	 */
	public String getFormulaFragment();
}
