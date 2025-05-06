/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.complete;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.FetchBuilderBasicValued;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.Collections;
import java.util.List;

/**
 * @author Christian Beikov
 */
public class DelayedFetchBuilderBasicPart
		implements CompleteFetchBuilder, FetchBuilderBasicValued, ModelPartReferenceBasic {
	private final NavigablePath navigablePath;
	private final BasicValuedModelPart referencedModelPart;
	private final boolean isEnhancedForLazyLoading;

	public DelayedFetchBuilderBasicPart(
			NavigablePath navigablePath,
			BasicValuedModelPart referencedModelPart,
			boolean isEnhancedForLazyLoading) {
		this.navigablePath = navigablePath;
		this.referencedModelPart = referencedModelPart;
		this.isEnhancedForLazyLoading = isEnhancedForLazyLoading;
	}

	@Override
	public FetchBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public BasicValuedModelPart getReferencedPart() {
		return referencedModelPart;
	}

	@Override
	public BasicFetch<?> buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState) {
		return new BasicFetch<>(
				-1,
				parent,
				fetchPath,
				referencedModelPart,
				null,
				FetchTiming.DELAYED,
				isEnhancedForLazyLoading,
				domainResultCreationState,
				false,
				false
		);
	}

	@Override
	public List<String> getColumnAliases() {
		return Collections.emptyList();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final DelayedFetchBuilderBasicPart that = (DelayedFetchBuilderBasicPart) o;
		return isEnhancedForLazyLoading == that.isEnhancedForLazyLoading
			&& navigablePath.equals( that.navigablePath )
			&& referencedModelPart.equals( that.referencedModelPart );
	}

	@Override
	public int hashCode() {
		int result = navigablePath.hashCode();
		result = 31 * result + referencedModelPart.hashCode();
		result = 31 * result + ( isEnhancedForLazyLoading ? 1 : 0 );
		return result;
	}
}
