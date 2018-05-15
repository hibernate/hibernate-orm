/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.UUID;

import org.hibernate.dialect.Dialect;

/**
 * Represents a TableSpecification derived from a subquery (inline view), as opposed to a PhysicalTableSpecification
 *
 * @author Steve Ebersole
 */
public class DerivedTable extends AbstractTable {
	private final String expression;

	public DerivedTable(UUID uuid, String expression, boolean isAbstract) {
		super( uuid, isAbstract );
		this.expression = expression;
	}

	@Override
	public String getTableExpression() {
		return expression;
	}

	@Override
	public String render(Dialect dialect) {
		return expression;
	}

	@Override
	public boolean isExportable() {
		return false;
	}

	@Override
	public String toString() {
		return "DerivedTable(" + expression + ")";
	}

	@Override
	public String toLoggableFragment() {
		return "<subselect ...>";
	}
}
