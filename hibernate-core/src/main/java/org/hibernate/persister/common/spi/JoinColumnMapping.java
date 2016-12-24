/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.spi;

/**
 * Represents one column mapping as part of a join restriction
 * from a mapped association.
 * <p/>
 * NOTE left-hand-side and right-hand-side here are relative to the owner and
 *
 *
 * @author Steve Ebersole
 */
public class JoinColumnMapping {
	private final Column lhs;
	private final Column rhs;

	public JoinColumnMapping(Column lhs, Column rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public Column getLeftHandSideColumn() {
		return lhs;
	}

	public Column getRightHandSideColumn() {
		return rhs;
	}
}
