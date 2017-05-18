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
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionElementEntity;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.sql.JoinType;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
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
	private final EntityTypeImplementor<J> entityPersister;

	public CollectionElementEntityImpl(
			PersistentCollectionMetadata persister,
			Collection mappingBinding,
			ElementClassification elementClassification,
			RuntimeModelCreationContext creationContext) {
		super( persister );
		this.elementClassification = elementClassification;

		final ToOne value = (ToOne) mappingBinding.getElement();
		this.entityPersister = creationContext.getTypeConfiguration().findEntityPersister( value.getReferencedEntityName() );
	}

	@Override
	public EntityTypeImplementor<J> getEntityPersister() {
		return entityPersister;
	}

	@Override
	public String getEntityName() {
		return getEntityPersister().getEntityName();
	}

	@Override
	public String getJpaEntityName() {
		return getEntityPersister().getJpaEntityName();
	}

	@Override
	public ElementClassification getClassification() {
		return elementClassification;
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getEntityPersister().findNavigable( navigableName );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return getEntityPersister().findDeclaredNavigable( navigableName );
	}

	@Override
	public List<Navigable> getNavigables() {
		return getEntityPersister().getNavigables();
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		return getEntityPersister().getDeclaredNavigables();
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		getEntityPersister().visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		getEntityPersister().visitDeclaredNavigables( visitor );
	}

	@Override
	public EntityJavaDescriptor<J> getJavaTypeDescriptor() {
		return getEntityPersister().getJavaTypeDescriptor();
	}

	@Override
	public void applyTableReferenceJoins(
			JoinType joinType,
			SqlAliasBaseManager.SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector collector) {
		getEntityPersister().applyTableReferenceJoins( joinType, sqlAliasBase, collector );
	}

	@Override
	public List<Column> getColumns() {
		throw new NotYetImplementedException();
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceResolver,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceSource,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		// delegate to the persister because here we are returning
		// 		the entities that make up the referenced collection's elements
		return getEntityPersister().generateQueryResult(
				selectedExpression,
				resultVariable,
				columnReferenceSource,
				sqlSelectionResolver,
				creationContext
		);
	}
}
