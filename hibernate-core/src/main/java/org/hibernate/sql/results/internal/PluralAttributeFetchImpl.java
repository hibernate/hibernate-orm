/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.InitializerCollector;
import org.hibernate.sql.results.spi.PluralAttributeFetch;
import org.hibernate.sql.results.spi.PluralAttributeInitializer;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeFetchImpl extends AbstractPluralAttributeMappingNode implements PluralAttributeFetch {
	private final FetchParent fetchParent;
	private final FetchStrategy fetchStrategy;
	private final ColumnReferenceQualifier qualifier;
	private final PluralPersistentAttribute pluralAttribute;
	private final QueryResultCreationContext creationContext;

	public PluralAttributeFetchImpl(
			FetchParent fetchParent,
			ColumnReferenceQualifier qualifier,
			PluralPersistentAttribute pluralAttribute,
			FetchStrategy fetchStrategy,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		super( pluralAttribute, resultVariable );
		this.fetchParent = fetchParent;
		this.qualifier = qualifier;
		this.pluralAttribute = pluralAttribute;
		this.fetchStrategy = fetchStrategy;
		this.creationContext = creationContext;

		fetchParent.addFetch( this );
	}

	@Override
	public ColumnReferenceQualifier getSqlExpressionQualifier() {
		return qualifier;
	}

	@Override
	public void registerInitializers(FetchParentAccess parentAccess, InitializerCollector collector) {
		final PluralAttributeInitializer initializer = new PluralAttributeFetchInitializer( pluralAttribute );

		// todo (6.0) : how to handle index/element?

		collector.addInitializer( initializer );
	}

	@Override
	public PluralPersistentAttribute getFetchedNavigable() {
		return getNavigable();
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
}
