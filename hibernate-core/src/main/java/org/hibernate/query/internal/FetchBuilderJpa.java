/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.function.BiFunction;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Steve Ebersole
 */
public class FetchBuilderJpa implements FetchBuilder {
	private final NavigablePath navigablePath;
	private final String attributePath;

	public FetchBuilderJpa(NavigablePath navigablePath, String attributePath) {
		this.navigablePath = navigablePath;
		this.attributePath = attributePath;
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		assert fetchPath.equals( navigablePath );
		assert fetchPath.getFullPath().endsWith( attributePath );

		final FetchableContainer container = parent.getReferencedMappingContainer();
		final Fetchable subPart = (Fetchable) container.findSubPart( fetchPath.getLocalName(), null );
		return subPart.generateFetch(
				parent,
				fetchPath,
				FetchTiming.IMMEDIATE,
				true,
				LockMode.READ,
				null,
				domainResultCreationState
		);
	}
}
