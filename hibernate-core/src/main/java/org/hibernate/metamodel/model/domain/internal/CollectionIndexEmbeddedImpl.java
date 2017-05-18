/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionIndex;
import org.hibernate.metamodel.model.domain.spi.CollectionIndexEmbedded;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeImplementor;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.sql.ast.produce.result.internal.QueryResultCompositeImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class CollectionIndexEmbeddedImpl<J>
		extends AbstractCollectionIndex<J>
		implements CollectionIndexEmbedded<J> {
	private final EmbeddedTypeImplementor<J> embeddedPersister;
	private final List<Column> columns;

	public CollectionIndexEmbeddedImpl(
			PersistentCollectionMetadata persister,
			IndexedCollection mapping,
			RuntimeModelCreationContext creationContext) {
		super( persister );

		this.embeddedPersister = creationContext.getPersisterFactory().createEmbeddablePersister(
				(EmbeddedValueMapping) mapping.getIndex(),
				persister,
				NAVIGABLE_NAME,
				creationContext
		);

		this.columns = getEmbeddedPersister().collectColumns();

	}

	@Override
	public EmbeddedTypeImplementor<J> getEmbeddedPersister() {
		return embeddedPersister;
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getEmbeddedPersister().findNavigable( navigableName );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return getEmbeddedPersister().findDeclaredNavigable( navigableName );
	}

	@Override
	public List<Navigable> getNavigables() {
		return getEmbeddedPersister().getNavigables();
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		return getEmbeddedPersister().getDeclaredNavigables();
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		getEmbeddedPersister().visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		getEmbeddedPersister().visitDeclaredNavigables( visitor );
	}

	@Override
	public EmbeddableJavaDescriptor<J> getJavaTypeDescriptor() {
		return getEmbeddedPersister().getJavaTypeDescriptor();
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceSource,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return new QueryResultCompositeImpl( selectedExpression, resultVariable, getEmbeddedPersister() );
	}

	@Override
	public List<Column> getColumns() {
		return columns;
	}
}
