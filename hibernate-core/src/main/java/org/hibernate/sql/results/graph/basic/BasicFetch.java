/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.basic;

import java.util.BitSet;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.UnfetchedBasicPartResultAssembler;
import org.hibernate.sql.results.graph.UnfetchedResultAssembler;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Fetch for a basic-value
 *
 * @author Steve Ebersole
 */
public class BasicFetch<T> implements Fetch, BasicResultGraphNode<T> {
	private final NavigablePath navigablePath;
	private final FetchParent fetchParent;
	private final BasicValuedModelPart valuedMapping;

	private final DomainResultAssembler<T> assembler;

	private final FetchTiming fetchTiming;

	public BasicFetch(
			int valuesArrayPosition,
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			BasicValuedModelPart valuedMapping,
			FetchTiming fetchTiming,
			boolean unwrapRowProcessingState) {
		//noinspection unchecked
		this(
				valuesArrayPosition,
				fetchParent,
				fetchablePath,
				valuedMapping,
				(BasicValueConverter<T, ?>) valuedMapping.getJdbcMapping().getValueConverter(),
				fetchTiming,
				true,
				false,
				unwrapRowProcessingState
		);
	}

	public BasicFetch(
			int valuesArrayPosition,
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			BasicValuedModelPart valuedMapping,
			BasicValueConverter<T, ?> valueConverter,
			FetchTiming fetchTiming,
			boolean canBasicPartFetchBeDelayed,
			boolean coerceResultType,
			boolean unwrapRowProcessingState) {
		this.navigablePath = fetchablePath;

		this.fetchParent = fetchParent;
		this.valuedMapping = valuedMapping;
		this.fetchTiming = fetchTiming;
		@SuppressWarnings("unchecked")
		final var javaType = (JavaType<T>) valuedMapping.getJavaType();
		// lazy basic attribute
		if ( fetchTiming == FetchTiming.DELAYED && valuesArrayPosition == -1 ) {
			this.assembler = canBasicPartFetchBeDelayed
					? new UnfetchedResultAssembler<>( javaType )
					: new UnfetchedBasicPartResultAssembler<>( javaType );
		}
		else {
			this.assembler = coerceResultType
					? new CoercingResultAssembler<>( valuesArrayPosition, javaType, valueConverter, unwrapRowProcessingState )
					: new BasicResultAssembler<>( valuesArrayPosition, javaType, valueConverter, unwrapRowProcessingState );
		}
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
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public Fetchable getFetchedMapping() {
		return valuedMapping;
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return null;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public DomainResultAssembler<T> createAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return assembler;
	}

	@Override
	public DomainResultAssembler<T> createResultAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		return assembler;
	}

	@Override
	public String getResultVariable() {
		// a basic value used as a fetch will never have a result variable in the domain result
		return null;
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		if ( assembler instanceof BasicResultAssembler<T> basicResultAssembler ) {
			valueIndexes.set( basicResultAssembler.valuesArrayPosition );
		}
	}
}
