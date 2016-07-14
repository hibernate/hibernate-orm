/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.internal;

import org.hibernate.persister.common.spi.AbstractTable;
import org.hibernate.persister.common.spi.Table;

/**
 * Represents a TableSpecification derived from a subquery (inline view), as opposed to a PhysicalTableSpecification
 *
 * @author Steve Ebersole
 */
public class DerivedTable extends AbstractTable implements Table {
	private final String expression;

	public DerivedTable(String expression) {
		this.expression = expression;
	}

	@Override
	public String getTableExpression() {
		return expression;
	}

	@Override
	public String toString() {
		return "DeridedTable(" + expression + ")";
	}
}
