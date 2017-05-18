/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.select.EntityValuedSelectable;
import org.hibernate.sql.ast.tree.spi.select.Selectable;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityReference implements NavigableContainerReference {
	// todo (6.0) : for now assuming a single class works for all TableGroup Expression cases
	//		^^ verify that this is accurate and that there are no other pieces of information
	//		that we need to account for in the distinction.  See also EntityTableGroup for
	//		more details

	// todo (6.0) : see org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference and SqmRoot ctor

	private final EntityTypeImplementor entityPersister;
	private final NavigableContainerReference navigableContainerReference;
	private final EntityValuedExpressableType expressionType;
	private final NavigablePath propertyPath;

	private final EntityValuedSelectable selectable;

	private ColumnReferenceSource columnReferenceSource;

	/**
	 * @see NavigableReference#getContributedColumnReferenceSource
	 */
	public EntityReference(
			ColumnReferenceSource columnReferenceSource,
			EntityTypeImplementor entityPersister,
			NavigablePath propertyPath,
			boolean isShallow) {
		this( columnReferenceSource, entityPersister, null, entityPersister, propertyPath, isShallow );
	}

	/**
	 * @see NavigableReference#getContributedColumnReferenceSource
	 */
	public EntityReference(
			ColumnReferenceSource columnReferenceSource,
			EntityTypeImplementor entityPersister,
			NavigableContainerReference navigableContainerReference,
			EntityValuedExpressableType expressionType,
			NavigablePath propertyPath,
			boolean isShallow) {
		this.columnReferenceSource = columnReferenceSource;
		this.entityPersister = entityPersister;
		this.navigableContainerReference = navigableContainerReference;
		this.expressionType = expressionType;
		this.propertyPath = propertyPath;

		this.selectable = new EntityValuedSelectable(
				this,
				propertyPath,
				columnReferenceSource,
				entityPersister,
				isShallow
		);
	}

	public EntityTypeImplementor getEntityPersister() {
		return entityPersister;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return propertyPath;
	}

	@Override
	public EntityTypeImplementor getNavigable() {
		return getEntityPersister();
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
	public NavigableContainerReference getNavigableContainerReference() {
		return navigableContainerReference;
	}

	@Override
	public ColumnReferenceSource getContributedColumnReferenceSource() {
		return columnReferenceSource;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitEntityExpression( this );
	}
}
