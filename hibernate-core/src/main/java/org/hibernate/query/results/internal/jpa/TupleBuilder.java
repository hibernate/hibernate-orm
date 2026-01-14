/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.jpa;

import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import jakarta.persistence.sql.TupleMapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.results.spi.ResultBuilder;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Steve Ebersole
 */
public class TupleBuilder extends AbstractMappingElementBuilder<Tuple> implements ResultBuilder {
	private final TupleMetadata tupleMetadata;
	private final ResultBuilder[] elementBuilders;

	public TupleBuilder(TupleMapping tupleMapping, SessionFactoryImplementor sessionFactory) {
		super( null, sessionFactory.getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( Tuple.class ), sessionFactory );

		this.elementBuilders = new ResultBuilder[ tupleMapping.elements().length ];
		final TupleMetadataBuilder metadataBuilder = new TupleMetadataBuilder( tupleMapping.elements().length );
		for ( int i = 0; i < tupleMapping.elements().length; i++ ) {
			metadataBuilder.add( tupleMapping.elements()[i] );
			elementBuilders[i] = JpaMappingHelper.toHibernateBuilder(
					tupleMapping.elements()[i],
					sessionFactory
			);
		}
		this.tupleMetadata = metadataBuilder.buildMetadata();
	}

	@Override
	public DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState creationState) {
		final DomainResult<?>[] elementResults = new DomainResult<?>[elementBuilders.length];
		for ( int i = 0; i < elementBuilders.length; i++ ) {
			elementResults[i] = elementBuilders[i].buildResult( jdbcResultsMetadata, resultPosition, creationState );
		}
		return new TupleResultImpl( javaType, tupleMetadata, elementResults );
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
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
}
