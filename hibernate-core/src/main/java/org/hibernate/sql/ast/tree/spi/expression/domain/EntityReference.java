/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.ast.tree.spi.select.EntityValuedSelectable;
import org.hibernate.sql.ast.tree.spi.select.Selectable;

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

	private final EntityValuedExpressableType expressionType;

	private final EntityValuedSelectable selectable;

	public EntityReference(
			ColumnReferenceSource columnReferenceSource,
			EntityValuedExpressableType expressionType,
			NavigablePath navigablePath,
			NavigableContainerReference containerReference,
			boolean isShallow) {
		super( containerReference, expressionType, navigablePath );
		this.expressionType = expressionType;

		this.selectable = new EntityValuedSelectable(
				this,
				navigablePath,
				columnReferenceSource,
				isShallow
		);
	}

	@Override
	public EntityValuedExpressableType getType() {
		return expressionType;
	}

	@Override
	public Selectable getSelectable() {
		return selectable;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitEntityExpression( this );
	}
}
