/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.complete;

import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.internal.ResultMementoTuple;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.internal.TupleImpl;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.BitSet;
import java.util.function.Consumer;

/// [ResultBuilder] for handling JPA [jakarta.persistence.sql.TupleMapping].
///
/// @see org.hibernate.query.internal.ResultMementoTuple
///
/// @author Steve Ebersole
public class ResultBuilderTuple implements CompleteResultBuilder {
	private final JavaType<Tuple> javaType;
	private final TupleMetadata tupleMetadata;
	private final ResultBuilder[] elementBuilders;

	private ResultBuilderTuple(JavaType<Tuple> javaType, TupleMetadata tupleMetadata, ResultBuilder[] elementBuilders) {
		this.javaType = javaType;
		this.tupleMetadata = tupleMetadata;
		this.elementBuilders = elementBuilders;
	}

	public static ResultBuilder from(
			ResultMementoTuple resultMementoTuple,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		final ResultMementoTuple.Element[] elements = resultMementoTuple.getElements();

		final ResultBuilder[] elementBuilders = new ResultBuilder[ elements.length ];
		final TupleMetadataBuilder metadataBuilder = new TupleMetadataBuilder( elements.length );

		for ( int i = 0; i < elements.length; i++ ) {
			elementBuilders[i] = elements[i].resultMemento().resolve( querySpaceConsumer, context );
			metadataBuilder.add( elements[i].tupleElement() );
		}
		return new ResultBuilderTuple(
				context.getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( Tuple.class ),
				metadataBuilder.buildMetadata(),
				elementBuilders
		);
	}

	@Override
	public Class<?> getJavaType() {
		return Tuple.class;
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		final DomainResult<?>[] elementResults = new DomainResult<?>[elementBuilders.length ];
		for ( int i = 0; i < elementBuilders.length; i++ ) {
			elementResults[i] = elementBuilders[i].buildResult( jdbcResultsMetadata, resultPosition, domainResultCreationState );
		}
		return new TupleResult( javaType, tupleMetadata, elementResults );
	}

	private static class TupleMetadataBuilder {
		private final TupleElement<?>[] elements;
		private final String[] aliases;

		private int collectionPosition;

		public TupleMetadataBuilder(int elementCount) {
			elements = new TupleElement<?>[elementCount];
			aliases = new String[elementCount];
		}

		public void add(TupleElement<?> element) {
			elements[collectionPosition] = element;
			aliases[collectionPosition] = StringHelper.nullIfBlank( element.getAlias() );
			collectionPosition++;
		}

		public TupleMetadata buildMetadata() {
			return new TupleMetadata( elements, aliases );
		}
	}

	private record TupleResult(
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
		}

	private record TupleAssembler(
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
