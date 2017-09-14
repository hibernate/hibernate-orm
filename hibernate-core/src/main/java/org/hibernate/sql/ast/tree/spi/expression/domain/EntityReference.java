/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityReference extends AbstractNavigableContainerReference {
	// todo (6.0) : for now assuming a single class works for all TableGroup Expression cases
	//		^^ verify that this is accurate and that there are no other pieces of information
	//		that we need to account for in the distinction.  See also EntityTableGroup for
	//		more details

	// todo (6.0) : see org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference and SqmRoot ctor

	private final ColumnReferenceQualifier columnReferenceSource;
	private final EntityValuedExpressableType expressionType;

	public EntityReference(
			ColumnReferenceQualifier columnReferenceSource,
			EntityValuedExpressableType expressionType,
			NavigablePath navigablePath,
			NavigableContainerReference containerReference,
			boolean isShallow) {
		super( containerReference, expressionType, navigablePath );
		this.columnReferenceSource = columnReferenceSource;
		this.expressionType = expressionType;
	}

	@Override
	public EntityValuedNavigable getNavigable() {
		return (EntityValuedNavigable) super.getNavigable();
	}

	@Override
	public ColumnReferenceQualifier getSqlExpressionQualifier() {
		return columnReferenceSource;
	}

	@Override
	public EntityValuedExpressableType getType() {
		return expressionType;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitEntityExpression( this );
	}
}
