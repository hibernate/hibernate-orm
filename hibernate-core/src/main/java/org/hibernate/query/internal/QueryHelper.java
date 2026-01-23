/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import jakarta.persistence.TupleElement;
import jakarta.persistence.criteria.CompoundSelection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.spi.ResultSetMapping;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.named.NamedSqmQueryMemento;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.results.internal.TupleMetadata;

import java.util.List;
import java.util.function.Consumer;

import static org.hibernate.query.sqm.internal.SqmUtil.isHqlTuple;
import static org.hibernate.query.sqm.internal.SqmUtil.isSelectionAssignableToResultType;

/**
 * @author Steve Ebersole
 */
public class QueryHelper {
	private QueryHelper() {
		// disallow direct instantiation
	}

	@SafeVarargs
	public static <T> @Nullable SqmBindableType<? extends T> highestPrecedenceType(@Nullable SqmBindableType<? extends T>... types) {
		if ( types.length == 0 ) {
			return null;
		}

		if ( types.length == 1 ) {
			return types[0];
		}

		var highest = highestPrecedenceType2( types[0], types[1] );
		for ( int i = 2; i < types.length; i++ ) {
			highest = highestPrecedenceType2( highest, types[i] );
		}

		return highest;
	}

	public static <X> @Nullable SqmBindableType<? extends X> highestPrecedenceType2(
			@Nullable SqmBindableType<? extends X> type1,
			@Nullable SqmBindableType<? extends X> type2) {
		if ( type1 == null && type2 == null ) {
			return null;
		}
		else if ( type1 == null ) {
			return type2;
		}
		else if ( type2 == null ) {
			return type1;
		}

		if ( type1 instanceof SqmPathSource ) {
			return type1;
		}

		if ( type2 instanceof SqmPathSource ) {
			return type2;
		}

		if ( type1.getExpressibleJavaType() == null ) {
			return type2;
		}
		else if ( type2.getExpressibleJavaType() == null ) {
			return type1;
		}
		// any other precedence rules?
		else if ( type2.getExpressibleJavaType().isWider( type1.getExpressibleJavaType() ) ) {
			return type2;
		}

		return type1;
	}

	@SuppressWarnings("unchecked")
	public static <R> Class<R> determineResultType(SqmSelectStatement<?> sqm, Class<?> expectedResultType) {
		final var selections = sqm.getQuerySpec().getSelectClause().getSelections();
		if ( selections.size() == 1 ) {
			if ( Object[].class.equals( expectedResultType ) ) {
				// for JPA compatibility
				return (Class<R>) Object[].class;
			}
			else {
				final var selection = selections.get( 0 );
				if ( isSelectionAssignableToResultType( selection, expectedResultType ) ) {
					final var nodeJavaType = selection.getNodeJavaType();
					if ( nodeJavaType != null ) {
						return (Class<R>) nodeJavaType.getJavaTypeClass();
					}
				}
				// let's assume there's some way to instantiate it
				return (Class<R>) expectedResultType;
			}
		}
		else if ( expectedResultType != null ) {
			// assume we can repackage the tuple as the given type - worry
			// about how later (it's handled using a RowTransformer which is
			// set up in ConcreteSqmSelectQueryPlan.determineRowTransformer)
			return (Class<R>) expectedResultType;
		}
		else {
			// for JPA compatibility
			return (Class<R>) Object[].class;
		}
	}

	public static TupleMetadata buildTupleMetadata(SqmStatement<?> statement, Class<?> resultType, TupleTransformer<?> rowTransformer) {
		if ( statement instanceof SqmSelectStatement<?> select ) {
			final var selections = select.getQueryPart().getFirstQuerySpec().getSelectClause().getSelections();
			return isTupleMetadataRequired( resultType, selections )
					? getTupleMetadata( selections, rowTransformer )
					: null;
		}
		else {
			return null;
		}
	}

	private static <R> boolean isTupleMetadataRequired(Class<R> resultType, List<SqmSelection<?>> selections) {
		final var selection = selections.size() == 1 ? selections.get( 0 ) : null;
		return isHqlTuple( selection )
			|| !isInstantiableWithoutMetadata( resultType )
				&& !isSelectionAssignableToResultType( selection, resultType );
	}

	private static boolean isInstantiableWithoutMetadata(Class<?> resultType) {
		return resultType == null
			|| resultType.isArray()
			|| Object.class == resultType
			|| List.class == resultType;
	}

	private static TupleMetadata getTupleMetadata(List<SqmSelection<?>> selections, TupleTransformer<?> rowTransformer) {
		if ( rowTransformer == null ) {
			return new TupleMetadata( buildTupleElementArray( selections ), buildTupleAliasArray( selections ) );
		}
		else {
			throw new IllegalArgumentException(
					"Illegal combination of Tuple resultType and (non-JpaTupleBuilder) TupleTransformer: "
							+ rowTransformer
			);
		}
	}

	private static TupleElement<?>[] buildTupleElementArray(List<SqmSelection<?>> selections) {
		final int selectionsSize = selections.size();
		if ( selectionsSize == 1 ) {
			final var selectableNode = selections.get( 0 ).getSelectableNode();
			if ( selectableNode instanceof CompoundSelection<?> ) {
				final var selectionItems = selectableNode.getSelectionItems();
				final int itemsSize = selectionItems.size();
				final var elements = new TupleElement<?>[itemsSize];
				for ( int i = 0; i < itemsSize; i++ ) {
					elements[i] = selectionItems.get( i );
				}
				return elements;
			}
			else {
				return new TupleElement<?>[] { selectableNode };
			}
		}
		else {
			final var elements = new TupleElement<?>[selectionsSize];
			for ( int i = 0; i < selectionsSize; i++ ) {
				elements[i] = selections.get( i ).getSelectableNode();
			}
			return elements;
		}
	}

	private static String[] buildTupleAliasArray(List<SqmSelection<?>> selections) {
		final int selectionsSize = selections.size();
		if ( selectionsSize == 1 ) {
			final var selectableNode = selections.get(0).getSelectableNode();
			if ( selectableNode instanceof CompoundSelection<?> ) {
				final var selectionItems = selectableNode.getSelectionItems();
				final int itemsSize = selectionItems.size();
				final var elements = new String[itemsSize];
				for ( int i = 0; i < itemsSize; i++ ) {
					elements[i] = selectionItems.get( i ).getAlias();
				}
				return elements;
			}
			else {
				return new String[] { selectableNode.getAlias() };
			}
		}
		else {
			final String[] elements = new String[selectionsSize];
			for ( int i = 0; i < selectionsSize; i++ ) {
				elements[i] = selections.get( i ).getAlias();
			}
			return elements;
		}
	}

	public static ResultSetMapping resolveFromMemento(@Nullable NamedResultSetMappingMemento mappingMemento, Consumer<String> querySpaceConsumer, ResultSetMappingResolutionContext resolutionContext, SessionFactoryImplementor sessionFactory) {
		var resultSetMappingProducer = sessionFactory.getJdbcValuesMappingProducerProvider();
		var resultSetMapping = resultSetMappingProducer.buildResultSetMapping(
				mappingMemento.getName(),
				true,
				sessionFactory
		);
		mappingMemento.resolve( resultSetMapping, querySpaceConsumer, resolutionContext );
		return resultSetMapping;
	}

	public static <R> Class<R> determineResultType(Class<R> explicitType, ResultSetMapping resultSetMapping) {
		if ( explicitType != null ) {
			return explicitType;
		}

		assert resultSetMapping.getResultBuilders() != null;
		if ( resultSetMapping.getResultBuilders().isEmpty() ) {
			//noinspection unchecked
			return (Class<R>) Object.class;
		}
		else if ( resultSetMapping.getResultBuilders().size() > 1 ) {
			//noinspection unchecked
			return (Class<R>) Object[].class;
		}
		else {
			//noinspection unchecked
			return (Class<R>) resultSetMapping.getResultBuilders().get( 0 ).getJavaType();
		}
	}


	public static int @Nullable [] unnamedParameterIndices(DomainParameterXref domainParameterXref) {
		final var jpaCriteriaParamResolutions = domainParameterXref
				.getParameterResolutions()
				.getJpaCriteriaParamResolutions();
		if ( jpaCriteriaParamResolutions.isEmpty() ) {
			return null;
		}
		int maxId = 0;
		for ( var criteriaWrapper : jpaCriteriaParamResolutions.values() ) {
			maxId = Math.max( maxId, criteriaWrapper.getCriteriaParameterId() );
		}
		final var unnamedParameterIndices = new int[maxId + 1];
		for ( var entry : jpaCriteriaParamResolutions.entrySet() ) {
			unnamedParameterIndices[entry.getValue().getCriteriaParameterId()] =
					entry.getValue().getUnnamedParameterId();
		}
		return unnamedParameterIndices;

	}

	public static <T> HqlInterpretation<T> interpretation(
			NamedSqmQueryMemento<?> memento,
			Class<T> expectedResultType,
			SharedSessionContractImplementor session) {
		final var queryEngine = session.getFactory().getQueryEngine();
		return queryEngine.getInterpretationCache()
				.resolveHqlInterpretation( memento.getHqlString(), expectedResultType,
						queryEngine.getHqlTranslator() );
	}
}
