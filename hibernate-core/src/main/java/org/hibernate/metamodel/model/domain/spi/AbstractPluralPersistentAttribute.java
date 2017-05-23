/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.consume.results.spi.Initializer;
import org.hibernate.sql.ast.consume.results.spi.InitializerCollector;
import org.hibernate.sql.ast.produce.result.internal.FetchCollectionAttributeImpl;
import org.hibernate.sql.ast.produce.result.internal.QueryResultCollectionImpl;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchCollectionAttribute;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.FetchableCollectionElement;
import org.hibernate.sql.ast.produce.result.spi.FetchableCollectionIndex;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.select.Selection;

/**
 * @author Steve Ebersole
 */
public class AbstractPluralPersistentAttribute<O,C,E> extends AbstractPersistentAttribute<O,C> implements PluralPersistentAttribute<O,C,E> {
	private final PersistentCollectionMetadata<O,C,E> collectionMetadata;

	@Override
	public PersistentCollectionMetadata<O,C,E> getPersistentCollectionMetadata() {
		return collectionMetadata;
	}

	@Override
	public Type<E> getElementType() {
		// NOTE : this is the JPA Type
		return getPersistentCollectionMetadata().getElementDescriptor();
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.PLURAL_ATTRIBUTE;
	}

	@Override
	public Class<E> getBindableJavaType() {
		return getPersistentCollectionMetadata().getElementDescriptor().getJavaType();
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		switch ( getPersistentCollectionMetadata().getElementDescriptor().getClassification() ) {
			case EMBEDDABLE:
			case BASIC: {
				return PersistentAttributeType.ELEMENT_COLLECTION;
			}
			case ONE_TO_MANY: {
				return PersistentAttributeType.ONE_TO_MANY;
			}
			case MANY_TO_MANY: {
				return PersistentAttributeType.MANY_TO_MANY;
			}
			case ANY: {
				throw new NotYetImplementedException(  );
			}
			default: {
				throw new UnsupportedOperationException(
						"Could not interpret JPA PersistentAttributeType for plural attribute : " +
								getNavigableRole().getFullPath()
				);
			}
		}
	}

	@Override
	public boolean isAssociation() {
		return getPersistentAttributeType() == PersistentAttributeType.MANY_TO_MANY
				|| getPersistentAttributeType() == PersistentAttributeType.ONE_TO_MANY;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getPersistentCollectionMetadata().findNavigable( navigableName );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return getPersistentCollectionMetadata().findDeclaredNavigable( navigableName );
	}

	@Override
	public List<Navigable> getNavigables() {
		return getPersistentCollectionMetadata().getNavigables();
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		return getPersistentCollectionMetadata().getDeclaredNavigables();
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		getPersistentCollectionMetadata().visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		getPersistentCollectionMetadata().visitDeclaredNavigables( visitor );
	}

	@Override
	public String getNavigableName() {
		return getPersistentCollectionMetadata().getNavigableName();
	}

	@Override
	public String asLoggableText() {
		return getPersistentCollectionMetadata().asLoggableText();
	}

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return new QueryResultCollectionImpl(
				selectedExpression,
				resultVariable,
				sqlSelectionResolver,
				creationContext
		);
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigableReference selectedExpression,
			FetchStrategy fetchStrategy,
			String resultVariable,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		assert selectedExpression.getNavigable() == this;
		return new FetchCollectionAttributeImpl(
				fetchParent,
				selectedExpression,
				fetchStrategy,
				resultVariable,
				sqlSelectionResolver,
				creationContext
		);
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public ManagedTypeImplementor<C> getFetchedManagedType() {
		throw new NotYetImplementedException(  );
	}
}
