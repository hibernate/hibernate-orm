/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification.internal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Timeout;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.QueryException;
import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.StatelessSession;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.Order;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.internal.SelectionQueryImpl;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.named.NamedSelectionMemento;
import org.hibernate.query.named.NamedSqmQueryMemento;
import org.hibernate.query.restriction.Path;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.query.spi.JpaTypedQueryReference;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.SelectionQueryImplementor;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.AbstractSqmSelectQuery;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hibernate.internal.util.ArgumentsHelper.bindReferenceArguments;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOAD_GRAPH;
import static org.hibernate.query.sqm.internal.SqmUtil.validateCriteriaQuery;
import static org.hibernate.query.sqm.tree.SqmCopyContext.noParamCopyContext;
import static org.hibernate.query.sqm.tree.SqmCopyContext.simpleContext;

/**
 * Standard implementation of {@link SelectionSpecification}.
 *
 * @author Steve Ebersole
 */
public class SelectionSpecificationImpl<T> implements SelectionSpecification<T>, JpaTypedQueryReference<T> {
	private final Class<T> resultType;
	private final String hql;
	private final CriteriaQuery<T> criteriaQuery;
	private final TypedQueryReference<? super T> typedQueryReference;
	private final List<BiConsumer<AbstractSqmSelectQuery<T>, SqmRoot<? extends T>>> specifications = new ArrayList<>();

	public SelectionSpecificationImpl(Class<T> resultType) {
		this.resultType = resultType;
		this.hql = null;
		this.criteriaQuery = null;
		this.typedQueryReference = null;
	}

	public SelectionSpecificationImpl(String hql, Class<T> resultType) {
		this.resultType = resultType;
		this.hql = hql;
		this.criteriaQuery = null;
		this.typedQueryReference = null;
	}

	public SelectionSpecificationImpl(CriteriaQuery<T> criteriaQuery) {
		this.resultType = criteriaQuery.getResultType();
		this.hql = null;
		this.criteriaQuery = criteriaQuery;
		this.typedQueryReference = null;
	}

	public SelectionSpecificationImpl(TypedQueryReference<? super T> typedQueryReference, Class<T> resultType) {
		this.resultType = resultType;
		this.hql = null;
		this.criteriaQuery = null;
		this.typedQueryReference = typedQueryReference;
	}

	@Override
	public String getName() {
		return typedQueryReference == null ? null : typedQueryReference.getName();
	}

	@Override
	public Class<T> getResultType() {
		return resultType;
	}

	@Override
	public Map<String,Object> getHints() {
		return typedQueryReference == null ? emptyMap() : typedQueryReference.getHints();
	}

	@Override
	public List<Class<?>> getParameterTypes() {
		return typedQueryReference == null ? null : typedQueryReference.getParameterTypes();
	}

	@Override
	public List<String> getParameterNames() {
		return typedQueryReference == null ? null : typedQueryReference.getParameterNames();
	}

	@Override
	public List<Object> getArguments() {
		return typedQueryReference == null ? null : typedQueryReference.getArguments();
	}

	@Override
	public Set<TypedQuery.Option> getOptions() {
		return typedQueryReference == null ? emptySet() : typedQueryReference.getOptions();
	}

	@Override
	public TypedQueryReference<T> reference() {
		return this;
	}

	public List<BiConsumer<AbstractSqmSelectQuery<T>, SqmRoot<? extends T>>> getSpecifications() {
		return specifications;
	}

	@Override
	public SelectionSpecification<T> restrict(Restriction<? super T> restriction) {
		specifications.add( (sqmStatement, root) -> {
			final var sqmPredicate = SqmUtil.restriction( sqmStatement, resultType, restriction );
			sqmStatement.getQuerySpec().applyPredicate( sqmPredicate );
		} );
		return this;
	}

	@Override
	public SelectionSpecification<T> augment(Augmentation<T> augmentation) {
		specifications.add( (sqmStatement, root) -> {
			if ( !( sqmStatement instanceof SqmSelectStatement<T> selectStatement ) ) {
				throw new IllegalStateException( "Cannot augment a subquery" );
			}
			augmentation.augment( sqmStatement.nodeBuilder(), selectStatement, root );
		} );
		return this;
	}

	@Override
	public SelectionSpecification<T> fetch(Path<T, ?> fetchPath) {
		specifications.add( (sqmStatement, root) -> fetchPath.fetch( root ) );
		return this;
	}

	@Override
	public SelectionSpecification<T> sort(Order<? super T> order) {
		specifications.add( (sqmStatement, root) -> addOrder( order, sqmStatement ) );
		return this;
	}

	@Override
	public final SelectionSpecification<T> resort(Order<? super T> order) {
		specifications.add( (sqmStatement, root) -> {
			sqmStatement.getQuerySpec().setOrderByClause( new SqmOrderByClause() );
			addOrder( order, sqmStatement );
		} );
		return this;
	}

	@Override
	public final SelectionSpecification<T> resort(List<? extends Order<? super T>> orders) {
		specifications.add( (sqmStatement, root) -> {
			sqmStatement.getQuerySpec().setOrderByClause( new SqmOrderByClause() );
			orders.forEach( order -> addOrder( order, sqmStatement ) );
		} );
		return this;
	}

	private static <T> void addOrder(Order<? super T> order, AbstractSqmSelectQuery<T> sqmStatement) {
		final var sortSpecification = SqmUtil.sortSpecification( sqmStatement, order );
		final var querySpec = sqmStatement.getQuerySpec();
		if ( querySpec.getOrderByClause() == null ) {
			querySpec.setOrderByClause( new SqmOrderByClause() );
		}
		querySpec.getOrderByClause().addSortSpecification( sortSpecification );
	}

	@Override
	public SelectionQuery<T> createQuery(Session session) {
		return createQuery( (SharedSessionContract) session );
	}

	@Override
	public SelectionQuery<T> createQuery(StatelessSession session) {
		return createQuery( (SharedSessionContract) session );
	}

	public SelectionQueryImplementor<T> createQuery(SharedSessionContract session) {
		final var sessionImpl = session.unwrap(SharedSessionContractImplementor.class);
		final var buildResult = build( sessionImpl.getFactory().getQueryEngine() );
		final var query = createSelectionQuery( buildResult, sessionImpl );
		if ( typedQueryReference != null ) {
			bindReferenceArguments( query, typedQueryReference );
			setHintsAndOptions( query ); // arguably unnecessary
		}
		return query;
	}

	private @NonNull SelectionQueryImplementor<T> createSelectionQuery
			(SqmBuildResult<T> buildResult, SharedSessionContractImplementor session) {
		return buildResult.sqmMemento == null
				? new SelectionQueryImpl<>( buildResult.sqmStatement, false, resultType, session )
				: new SelectionQueryImpl<>( buildResult.sqmMemento, buildResult.sqmStatement, resultType, session );
	}

	private void setHintsAndOptions(SelectionQueryImplementor<T> query) {
		final var hints = typedQueryReference.getHints();
		if ( hints != null ) {
			hints.forEach( query::setHint );
		}

		final String entityGraphName = typedQueryReference.getEntityGraphName();
		if ( isNotEmpty( entityGraphName ) ) {
			query.setHint( HINT_SPEC_LOAD_GRAPH, entityGraphName );
		}

		typedQueryReference.getOptions().forEach( query::addOption );
	}

	private record SqmBuildResult<T>(
			SqmSelectStatement<T> sqmStatement,
			NamedSqmQueryMemento<T> sqmMemento) {
	}

	private SqmBuildResult<T> build(QueryEngine queryEngine) {
		final SqmSelectStatement<T> sqmStatement;
		final SqmRoot<? extends T> sqmRoot;
		final NamedSqmQueryMemento<T> sqmMemento;
		if ( hql != null ) {
			sqmStatement = resolveSqmTree( hql, resultType, queryEngine );
			sqmRoot = extractRoot( sqmStatement, resultType, hql );
			sqmMemento = null;
		}
		else if ( criteriaQuery != null ) {
			sqmStatement = ((SqmSelectStatement<T>) criteriaQuery).copy( simpleContext() );
			sqmRoot = extractRoot( sqmStatement, resultType, "criteria query" );
			sqmMemento = null;
		}
		else if ( typedQueryReference != null ) {
			if ( typedQueryReference instanceof SelectionSpecification<?> selectionSpecification ) {
				//noinspection unchecked
				sqmStatement = (SqmSelectStatement<T>)
						selectionSpecification.buildCriteria( queryEngine.getCriteriaBuilder() );
				sqmRoot = extractRoot( sqmStatement, resultType, "query reference" );
				sqmMemento = null;
			}
			else {
				sqmMemento = resolveSqmSelectionMemento( typedQueryReference, queryEngine );
				sqmStatement = resolveSqmTree( sqmMemento, resultType, queryEngine );
				sqmRoot = extractRoot( sqmStatement, resultType, sqmMemento.getHqlString() );
			}
		}
		else {
			var query = queryEngine.getCriteriaBuilder().createQuery( resultType );
			var root = query.from( resultType );
			query.select( root );
			sqmRoot = root;
			sqmStatement = query;
			sqmMemento = null;
		}
		specifications.forEach( consumer -> consumer.accept( sqmStatement, sqmRoot ) );
		return new SqmBuildResult<>( sqmStatement, sqmMemento );
	}

	@Override
	public SelectionQuery<T> createQuery(EntityManager entityManager) {
		return createQuery( (SharedSessionContract) entityManager );
	}

	@Override
	public CriteriaQuery<T> buildCriteria(CriteriaBuilder builder) {
		final var nodeBuilder = (NodeBuilder) builder;
		return build( nodeBuilder.getQueryEngine() ).sqmStatement;
	}

	@Override
	public SelectionSpecification<T> validate(CriteriaBuilder builder) {
		final var nodeBuilder = (NodeBuilder) builder;
		final var statement = build( nodeBuilder.getQueryEngine() ).sqmStatement;
		final var queryPart = statement.getQueryPart();
		// For criteria queries, we have to validate the fetch structure here
		queryPart.validateQueryStructureAndFetchOwners();
		validateCriteriaQuery( queryPart );
		statement.validateResultType( resultType );
		return this;
	}

	/**
	 * Used during construction to parse/interpret the incoming HQL
	 * and produce the corresponding SQM tree.
	 */
	private static <T> SqmSelectStatement<T> resolveSqmTree(String hql, Class<T> resultType, QueryEngine queryEngine) {
		final var hqlInterpretation =
				queryEngine.getInterpretationCache()
						.resolveHqlInterpretation( hql, resultType, queryEngine.getHqlTranslator() );
		if ( SqmUtil.isSelect( hqlInterpretation.getSqmStatement() ) ) {
			hqlInterpretation.validateResultType( resultType );
			// NOTE: this copy is to isolate the actual AST tree from the
			// one stored in the interpretation cache
			return (SqmSelectStatement<T>)
					hqlInterpretation.getSqmStatement()
							.copy( noParamCopyContext( SqmQuerySource.CRITERIA ) );
		}
		else {
			throw new IllegalSelectQueryException( "Expecting a selection query, but found '" + hql + "'", hql );
		}
	}

	/**
	 * Used during construction to resolve an incoming named query reference
	 * and produce the corresponding SQM tree.
	 */
	private static <T> SqmSelectStatement<T> resolveSqmTree(
			NamedSqmQueryMemento<?> sqmMemento,
			Class<T> resultType,
			QueryEngine queryEngine) {
		final var sqmStatement = sqmMemento.getSqmStatement();
		if ( sqmStatement == null ) {
			return resolveSqmTree( sqmMemento.getHqlString(), resultType, queryEngine );
		}
		else if ( SqmUtil.isSelect( sqmStatement ) ) {
			//noinspection unchecked
			return (SqmSelectStatement<T>) sqmStatement.copy( simpleContext() );
		}
		else {
			throw new IllegalSelectQueryException(
					"Expecting a selection query, but found '" + sqmMemento.getHqlString() + "'",
					sqmMemento.getHqlString()
			);
		}
	}

	private static <T> NamedSqmQueryMemento<T> resolveSqmSelectionMemento(
			TypedQueryReference<? super T> typedQueryReference,
			QueryEngine queryEngine) {
		final NamedQueryMemento<T> namedMemento =
				queryEngine.getNamedObjectRepository()
						//FIXME: unchecked cast
						.getQueryMementoByName( typedQueryReference.getName(), false );
		if ( namedMemento instanceof NamedSelectionMemento<T>
				&& namedMemento instanceof NamedSqmQueryMemento<T> sqmMemento ) {
			return sqmMemento;
		}
		else {
			throw new IllegalSelectQueryException(
					"SelectionSpecification only supports HQL or criteria query references: "
							+ typedQueryReference.getName()
			);
		}
	}

	/**
	 * Used during construction. Mainly used to group extracting and
	 * validating the root.
	 */
	private SqmRoot<? extends T> extractRoot(SqmSelectStatement<T> sqmStatement, Class<T> resultType, String hql) {
		final var sqmRoots = sqmStatement.getQuerySpec().getRoots();
		if ( sqmRoots.isEmpty() ) {
			throw new QueryException( "Query did not define any roots", hql );
		}
		if ( sqmRoots.size() > 1 ) {
			throw new QueryException( "Query defined multiple roots", hql );
		}

		final var sqmRoot = sqmRoots.iterator().next();
		return validateRoot( sqmRoot, resultType, hql );
		// for later, to support select lists
		//validateResultType( sqmStatement, sqmRoot, resultType, hql );
	}

	private SqmRoot<? extends T> validateRoot(SqmRoot<?> sqmRoot, Class<T> resultType, String hql) {
		final var javaType = sqmRoot.getJavaType();
		if ( javaType != null
				&& !Map.class.isAssignableFrom( javaType )
				&& !resultType.isAssignableFrom( javaType ) ) {
			throw new QueryException(
					String.format(
							Locale.ROOT,
							"Query root [%s] and result type [%s] are not compatible",
							javaType.getName(),
							resultType.getName()
					),
					hql
			);
		}
		else {
			@SuppressWarnings("unchecked") // Safe, we just checked
			final var castRoot = (SqmRoot<T>) sqmRoot;
			return castRoot;
		}
	}

//	/**
//	 * For future, allowing explicit select list.
//	 */
//	private void validateResultType(
//			SqmSelectStatement<T> sqmStatement,
//			SqmRoot<?> sqmRoot,
//			Class<T> resultType,
//			String hql) {
//		if ( resultType == null || Object.class.equals( resultType ) || resultType.isArray() ) {
//			// Nothing to validate in these cases
//			return;
//		}
//
//		final var rootJavaType = sqmRoot.getJavaType();
//		assert rootJavaType != null;
//
//		if ( Map.class.isAssignableFrom( rootJavaType ) ) {
//			if ( Map.class.isAssignableFrom( resultType ) ) {
//				// dynamic model and Map was requested, totally fine
//				return;
//			}
//		}
//
//		final var sqmSelectClause = sqmStatement.getQuerySpec().getSelectClause();
//		final var sqmSelections = sqmSelectClause.getSelections();
//		if ( isEmpty( sqmSelections ) ) {
//			// implicit select clause, verify that resultType matches the root type
//			if ( resultType.isAssignableFrom( rootJavaType ) ) {
//				// it does, we are fine
//				return;
//			}
//		}
//		else if ( sqmSelections.size() > 1 ) {
//			// we have to assume we can.
//			// the Query will ultimately complain if not, but this is the most we can do here
//			return;
//		}
//		else {
//			assert sqmSelections.size() == 1;
//			final var nodeJavaType = sqmSelections.get( 0 ).getNodeJavaType();
//			if ( nodeJavaType == null ) {
//				// again, we have to assume we can
//				return;
//			}
//			else if ( resultType.isAssignableFrom( nodeJavaType.getJavaTypeClass() ) ) {
//				// it matches the selection type, we are fine
//				return;
//			}
//		}
//
//		throw new QueryException(
//				String.format(
//						Locale.ROOT,
//						"Specified result-type [%s] is not valid for this SelectionSpecification",
//						resultType.getName()
//				),
//				hql
//		);
//	}

	@Override
	public Timeout getTimeout() {
		return typedQueryReference instanceof JpaTypedQueryReference<?> jpaTypedQueryReference
				? jpaTypedQueryReference.getTimeout()
				: null;
	}

	@Override
	public String getEntityGraphName() {
		return typedQueryReference == null ? "" : typedQueryReference.getEntityGraphName();
	}
}
