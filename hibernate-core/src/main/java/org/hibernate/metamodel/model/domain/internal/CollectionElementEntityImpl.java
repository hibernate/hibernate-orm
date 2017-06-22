/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionElementEntity;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.TableGroupContext;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class CollectionElementEntityImpl<J>
		extends AbstractCollectionElement<J>
		implements CollectionElementEntity<J> {

	private final ElementClassification elementClassification;
	private final EntityDescriptor<J> entityDescriptor;

	public CollectionElementEntityImpl(
			PersistentCollectionDescriptor persister,
			Collection mappingBinding,
			ElementClassification elementClassification,
			RuntimeModelCreationContext creationContext) {
		super( persister );
		this.elementClassification = elementClassification;

		final ToOne value = (ToOne) mappingBinding.getElement();
		this.entityDescriptor = creationContext.getTypeConfiguration().findEntityDescriptor( value.getReferencedEntityName() );
	}

	@Override
	public EntityDescriptor<J> getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public String getEntityName() {
		return getEntityDescriptor().getEntityName();
	}

	@Override
	public String getJpaEntityName() {
		return getEntityDescriptor().getJpaEntityName();
	}

	@Override
	public ElementClassification getClassification() {
		return elementClassification;
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getEntityDescriptor().findNavigable( navigableName );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return getEntityDescriptor().findDeclaredNavigable( navigableName );
	}

	@Override
	public List<Navigable> getNavigables() {
		return getEntityDescriptor().getNavigables();
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		return getEntityDescriptor().getDeclaredNavigables();
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		getEntityDescriptor().visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		getEntityDescriptor().visitDeclaredNavigables( visitor );
	}

	@Override
	public EntityJavaDescriptor<J> getJavaTypeDescriptor() {
		return getEntityDescriptor().getJavaTypeDescriptor();
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceSource lhs,
			org.hibernate.sql.ast.JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector,
			TableGroupContext tableGroupContext) {
		getEntityDescriptor().applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector, tableGroupContext );
	}

	@Override
	public List<Column> getColumns() {
		throw new NotYetImplementedException();
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		// delegate to the persister because here we are returning
		// 		the entities that make up the referenced collection's elements
		return getEntityDescriptor().generateQueryResult(
				selectedExpression,
				resultVariable,
				sqlSelectionResolver,
				creationContext
		);
	}
}
