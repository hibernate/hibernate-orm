/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionElementEmbedded;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.ast.produce.result.internal.QueryResultCompositeImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class CollectionElementEmbeddedImpl<J>
		extends AbstractCollectionElement<J>
		implements CollectionElementEmbedded<J> {

	private final EmbeddedTypeDescriptor<J> embeddedPersister;
	private final List<Column> columnList;

	public CollectionElementEmbeddedImpl(
			PersistentCollectionDescriptor persister,
			Collection mapping,
			RuntimeModelCreationContext creationContext) {
		super( persister );


		// todo (6.0) : transform the EmbeddedValueMapping representing the collection element into a EmbeddedPersister

		this.embeddedPersister = creationContext.getRuntimeModelDescriptorFactory().createEmbeddedTypeDescriptor(
				(EmbeddedValueMapping) mapping.getElement(),
				persister,
				NAVIGABLE_NAME,
				creationContext
		);
		this.columnList = embeddedPersister.collectColumns();
	}

	@Override
	public EmbeddedTypeDescriptor<J> getEmbeddedDescriptor() {
		return embeddedPersister;
	}

	@Override
	public EmbeddableJavaDescriptor<J> getJavaTypeDescriptor() {
		return getEmbeddedDescriptor().getJavaTypeDescriptor();
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getEmbeddedDescriptor().findNavigable( navigableName );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return getEmbeddedDescriptor().findDeclaredNavigable( navigableName );
	}

	@Override
	public List<Navigable> getNavigables() {
		return getEmbeddedDescriptor().getNavigables();
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		return embeddedPersister.getDeclaredNavigables();
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		// visit our sub-navigables
		getEmbeddedDescriptor().visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		// visit our declared sub-navigables
		getEmbeddedDescriptor().visitDeclaredNavigables( visitor );
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return new QueryResultCompositeImpl( selectedExpression, resultVariable, embeddedPersister );
	}

	@Override
	public List<Column> getColumns() {
		return columnList;
	}
}
