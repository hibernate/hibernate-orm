/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query.results.internal;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultSetMappingResolutionContext;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * Models the {@code "element"} pseudo-{@code <return-property/>}
 * for a collection-valued {@code <return-join/>}
 *
 * @implNote The "element" side of a collection fk is the "referring" side
 * in SQL terms
 *
 * @author Steve Ebersole
 */
public class HbmCollectionElementMemento implements FetchMemento {
	private final Builder builder;

	public HbmCollectionElementMemento(NavigablePath navigablePath, PluralAttributeMapping pluralAttr, List<String> columnAliases) {
		builder = new Builder( navigablePath, pluralAttr, columnAliases );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return builder.navigablePath;
	}

	@Override
	public FetchBuilder resolve(Parent parent, Consumer<String> querySpaceConsumer, ResultSetMappingResolutionContext context) {
		return builder;
	}

	private static class Builder implements FetchBuilder {
		private final NavigablePath navigablePath;
		private final PluralAttributeMapping pluralAttr;
		private final List<String> columnAliases;

		public Builder(NavigablePath navigablePath, PluralAttributeMapping pluralAttr, List<String> columnAliases) {
			this.navigablePath = navigablePath;
			this.pluralAttr = pluralAttr;
			this.columnAliases = columnAliases;
		}

		@Override
		public Fetch buildFetch(
				FetchParent parent,
				NavigablePath fetchPath,
				JdbcValuesMetadata jdbcResultsMetadata,
				BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
				DomainResultCreationState domainResultCreationState) {
			final ForeignKeyDescriptor keyDescriptor = pluralAttr.getKeyDescriptor();
			return ( (Fetchable) keyDescriptor.getKeyPart() ).generateFetch(
					parent,
					fetchPath,
					FetchTiming.IMMEDIATE,
					true,
					null,
					domainResultCreationState
			);
		}

		@Override
		public FetchBuilder cacheKeyInstance() {
			return this;
		}
	}
}
