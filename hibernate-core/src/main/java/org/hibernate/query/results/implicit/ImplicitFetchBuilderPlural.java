/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.implicit;

import java.util.function.BiFunction;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;

/**
 * @author Christian Beikov
 */
public class ImplicitFetchBuilderPlural implements ImplicitFetchBuilder {
	private final NavigablePath fetchPath;
	private final PluralAttributeMapping fetchable;

	public ImplicitFetchBuilderPlural(
			NavigablePath fetchPath,
			PluralAttributeMapping fetchable,
			DomainResultCreationState creationState) {
		this.fetchPath = fetchPath;
		this.fetchable = fetchable;
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState creationState) {
		final DomainResultCreationStateImpl creationStateImpl = impl( creationState );

		final Fetch fetch = parent.generateFetchableFetch(
				fetchable,
				fetchPath,
				FetchTiming.DELAYED,
				false,
				LockMode.READ,
				null,
				creationState
		);

		return fetch;
	}

	@Override
	public String toString() {
		return "ImplicitFetchBuilderPlural(" + fetchPath + ")";
	}
}
