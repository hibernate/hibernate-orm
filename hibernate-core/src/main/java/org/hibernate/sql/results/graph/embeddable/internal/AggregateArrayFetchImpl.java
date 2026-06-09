/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import java.util.BitSet;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.basic.BasicResultAssembler;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Fetches an aggregate embeddable array as the single plural aggregate column value.
 *
 * @author Steve Ebersole
 */
public class AggregateArrayFetchImpl implements Fetch {
	private final int valuesArrayPosition;
	private final FetchParent fetchParent;
	private final NavigablePath navigablePath;
	private final EmbeddableValuedModelPart fetchedMapping;
	private final JdbcMapping jdbcMapping;
	private final FetchTiming fetchTiming;
	private final boolean unwrapRowProcessingState;

	public AggregateArrayFetchImpl(
			int valuesArrayPosition,
			FetchParent fetchParent,
			NavigablePath navigablePath,
			EmbeddableValuedModelPart fetchedMapping,
			JdbcMapping jdbcMapping,
			FetchTiming fetchTiming,
			boolean unwrapRowProcessingState) {
		this.valuesArrayPosition = valuesArrayPosition;
		this.fetchParent = fetchParent;
		this.navigablePath = navigablePath;
		this.fetchedMapping = fetchedMapping;
		this.jdbcMapping = jdbcMapping;
		this.fetchTiming = fetchTiming;
		this.unwrapRowProcessingState = unwrapRowProcessingState;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public Fetchable getFetchedMapping() {
		return fetchedMapping;
	}

	@Override
	public FetchTiming getTiming() {
		return fetchTiming;
	}

	@Override
	public boolean hasTableGroup() {
		return fetchTiming == FetchTiming.IMMEDIATE;
	}

	@Override
	@SuppressWarnings("unchecked")
	public DomainResultAssembler<?> createAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return new BasicResultAssembler<>(
				valuesArrayPosition,
				(JavaType<Object>) jdbcMapping.getJavaTypeDescriptor(),
				(BasicValueConverter<Object, ?>) jdbcMapping.getValueConverter(),
				unwrapRowProcessingState
		);
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		valueIndexes.set( valuesArrayPosition );
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return jdbcMapping.getJavaTypeDescriptor();
	}

	@Override
	public boolean containsAnyNonScalarResults() {
		return false;
	}
}
