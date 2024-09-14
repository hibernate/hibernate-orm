/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.mutation.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;

/**
 * @author Steve Ebersole
 */
public class TableKeyExpressionCollector {

	private final EntityMappingType entityMappingType;
	private Expression firstColumnExpression;
	private List<Expression> collectedColumnExpressions;

	public TableKeyExpressionCollector(EntityMappingType entityMappingType) {
		this.entityMappingType = entityMappingType;
	}

	public void apply(ColumnReference columnReference) {
		if ( firstColumnExpression == null ) {
			firstColumnExpression = columnReference;
		}
		else if ( collectedColumnExpressions == null ) {
			collectedColumnExpressions = new ArrayList<>();
			collectedColumnExpressions.add( firstColumnExpression );
			collectedColumnExpressions.add( columnReference );
		}
		else {
			collectedColumnExpressions.add( columnReference );
		}
	}

	public Expression buildKeyExpression() {
		if ( collectedColumnExpressions == null ) {
			return firstColumnExpression;
		}

		return new SqlTuple( collectedColumnExpressions, entityMappingType.getIdentifierMapping() );
	}
}
