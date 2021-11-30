/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;

/**
 * @author Steve Ebersole
 */
class TableKeyExpressionCollector {
	private final EntityMappingType entityMappingType;

	TableKeyExpressionCollector(EntityMappingType entityMappingType) {
		this.entityMappingType = entityMappingType;
	}

	Expression firstColumnExpression;
	List<Expression> collectedColumnExpressions;

	void apply(ColumnReference columnReference) {
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

	Expression buildKeyExpression() {
		if ( collectedColumnExpressions == null ) {
			return firstColumnExpression;
		}

		return new SqlTuple( collectedColumnExpressions, entityMappingType.getIdentifierMapping() );
	}
}
