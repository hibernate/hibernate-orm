/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.from;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.DomainReferenceImplementor;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sql.ast.expression.domain.EntityReferenceExpression;
import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;
import org.hibernate.sql.exec.spi.SqlAstSelectInterpreter;

/**
 * A TableSpecificationGroup for an entity reference
 *
 * @author Steve Ebersole
 */
public class EntityTableGroup extends AbstractTableGroup implements Selectable {
	private final EntityPersister persister;

	private EntityReferenceExpression selectableExpression;
	private List<ColumnBinding> identifierColumnBindings;

	public EntityTableGroup(
			TableSpace tableSpace,
			String uid,
			String aliasBase,
			EntityPersister persister,
			PropertyPath propertyPath) {
		super( tableSpace, uid, aliasBase, propertyPath );

		this.persister = persister;
	}

	public List<ColumnBinding> resolveIdentifierColumnBindings() {
		if ( identifierColumnBindings == null ) {
			identifierColumnBindings = buildIdentifierColumnBindings();
		}
		return identifierColumnBindings;
	}

	private List<ColumnBinding> buildIdentifierColumnBindings() {
		final List<ColumnBinding> bindings = new ArrayList<>();

		for ( Column column : persister.getHierarchy().getIdentifierDescriptor().getColumns() ) {
			bindings.add( resolveColumnBinding( column ) );
		}
		return bindings;
	}

	public EntityPersister getPersister() {
		return persister;
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public void accept(SqlAstSelectInterpreter walker) {
		// todo : need a way to resolve ColumnBinding[] to SqlSelectable[]
		// walking a TableGroup as an Expression is likely wrong
		//throw new IllegalStateException( "Cannot treat TableGroup as Expression" );

		walker.visitEntityExpression( selectableExpression );
	}

	@Override
	public DomainReferenceImplementor getDomainReference() {
		return persister;
	}

	@Override
	public EntityReferenceExpression getSelectedExpression() {
		if ( selectableExpression == null ) {
			selectableExpression = new EntityReferenceExpression(
					this,
					persister,
					getPropertyPath(),
					// todo : (vv) shallow
					false
			);
		}
		return selectableExpression;
	}

	@Override
	public List<ColumnBinding> getColumnBindings() {
		return getSelectedExpression().getColumnBindings();
	}

	@Override
	public Return toQueryReturn(ReturnResolutionContext returnResolutionContext, String resultVariable) {
		return getSelectedExpression().getSelectable().toQueryReturn( returnResolutionContext, resultVariable );
	}
}
