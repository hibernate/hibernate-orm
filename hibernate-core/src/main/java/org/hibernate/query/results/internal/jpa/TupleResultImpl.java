/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.jpa;

import jakarta.persistence.Tuple;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.internal.TupleImpl;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.BitSet;

/**
 * @author Steve Ebersole
 */
public record TupleResultImpl(
		JavaType<Tuple> resultType,
		TupleMetadata tupleMetadata,
		DomainResult<?>[] elementResults) implements DomainResult<Tuple> {

	@Override
	public JavaType<?> getResultJavaType() {
		return resultType;
	}

	@Override
	public String getResultVariable() {
		return "";
	}

	@Override
	public TupleAssembler createResultAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState) {
		final DomainResultAssembler<?>[] elementAssemblers = new DomainResultAssembler<?>[elementResults.length];
		for ( int i = 0; i < elementResults.length; i++ ) {
			elementAssemblers[i] = elementResults[i].createResultAssembler( parent, creationState );
		}
		return new TupleAssembler( resultType, tupleMetadata, elementAssemblers );
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {

	}

	public record TupleAssembler(
			JavaType<Tuple> resultType,
			TupleMetadata tupleMetadata,
			DomainResultAssembler<?>[] elementAssemblers) implements DomainResultAssembler<Tuple> {

		@Override
		public JavaType<Tuple> getAssembledJavaType() {
			return resultType;
		}

		@Override
		public Tuple assemble(RowProcessingState rowProcessingState) {
			final Object[] row = new Object[elementAssemblers.length];
			for ( int i = 0; i < elementAssemblers.length; i++ ) {
				row[i] = elementAssemblers[i].assemble( rowProcessingState );
			}
			return new TupleImpl( tupleMetadata, row );
		}
	}
}
