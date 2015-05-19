/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;


/**
 * A disjunctive string of conditions
 * @author Gavin King
 */
public class DisjunctionFragment {
	private StringBuilder buffer = new StringBuilder();

	public DisjunctionFragment addCondition(ConditionFragment fragment) {
		addCondition( fragment.toFragmentString() );
		return this;
	}

	public DisjunctionFragment addCondition(String fragment) {
		if ( buffer.length() > 0 ) {
			buffer.append(" or ");
		}
		buffer.append( '(' )
				.append( fragment )
				.append( ')' );
		return this;
	}

	public String toFragmentString() {
		return buffer.toString();
	}
}
