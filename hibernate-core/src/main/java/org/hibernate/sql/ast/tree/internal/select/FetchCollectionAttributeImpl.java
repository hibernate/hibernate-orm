/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.internal.select;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.exec.results.spi.Initializer;
import org.hibernate.sql.exec.results.spi.InitializerCollector;
import org.hibernate.sql.ast.tree.spi.select.FetchCollectionAttribute;
import org.hibernate.sql.ast.tree.spi.select.FetchParent;
import org.hibernate.sql.ast.tree.spi.select.QueryResultCreationContext;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;

/**
 * @author Steve Ebersole
 */
public class FetchCollectionAttributeImpl extends AbstractCollectionReference implements FetchCollectionAttribute {
	private final FetchParent fetchParent;
	private final FetchStrategy fetchStrategy;
	private final SqlSelectionResolver sqlSelectionResolver;
	private final QueryResultCreationContext creationContext;

	public FetchCollectionAttributeImpl(
			FetchParent fetchParent,
			NavigableReference selectedExpression,
			FetchStrategy fetchStrategy,
			String resultVariable,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		super( selectedExpression, resultVariable );
		this.fetchParent = fetchParent;
		this.fetchStrategy = fetchStrategy;
		this.sqlSelectionResolver = sqlSelectionResolver;
		this.creationContext = creationContext;
	}

	@Override
	public String getResultVariable() {
		return super.getResultVariable();
	}

	@Override
	public void registerInitializers(InitializerCollector collector) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public Initializer getInitializer() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public boolean isNullable() {
		return true;
	}

	@Override
	public PluralPersistentAttribute getFetchedNavigable() {
		return (PluralPersistentAttribute) getNavigableReference().getNavigable();
	}
}
