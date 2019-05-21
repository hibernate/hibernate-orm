/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.cte;

import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.naming.Identifier;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Adapts a CTE "column name" to the {@link org.hibernate.metamodel.model.relational.spi.Column}
 * hierarchy for use inSQL AST
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class CteTableColumn extends PhysicalColumn {
	public CteTableColumn(
			CteTable table,
			PhysicalColumn sourceColumn,
			TypeConfiguration typeConfiguration) {
		super(
				table,
				Identifier.toIdentifier( "cte_" + sourceColumn.getName().render() ),
				() -> sourceColumn.getExpressableType().getSqlTypeDescriptor(),
				() -> sourceColumn.getExpressableType().getJavaTypeDescriptor(),
				null,
				null,
				false,
				true,
				typeConfiguration
		);
	}
}
