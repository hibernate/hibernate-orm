/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.query.Query;
import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.query.SemanticException;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.hql.internal.NamedHqlQueryMementoImpl;
import org.hibernate.query.hql.internal.QuerySplitter;
import org.hibernate.query.hql.spi.HqlQueryImplementor;
import org.hibernate.query.hql.spi.NamedHqlQueryMemento;
import org.hibernate.query.internal.DelegatingDomainQueryExecutionContext;
import org.hibernate.query.internal.ParameterMetadataImpl;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.internal.QueryParameterBindingsImpl;
import org.hibernate.query.spi.AbstractQuery;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.MutableQueryOptions;
import org.hibernate.query.spi.NonSelectQueryPlan;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryInterpretationCache;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.insert.SqmValues;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Tuple;

import static org.hibernate.query.spi.SqlOmittingQueryOptions.omitSqlQueryOptions;

/**
 * {@link Query} implementation based on an SQM
 *
 * @author Steve Ebersole
 */
public class QuerySqmImpl<R>
		extends AbstractQuery<R>
		implements HqlQueryImplementor<R>, DomainQueryExecutionContext {

	/**
	 * The value used for {@link #getQueryString} for Criteria-based queries
	 */
	public static final String CRITERIA_HQL_STRING = "<criteria>";
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( QuerySqmImpl.class );

	private final String hqlString;
	private final SqmStatement<R> sqmStatement;
	private final Class<R> resultType;

	private final ParameterMetadataImplementor parameterMetadata;
	private final DomainParameterXref domainParameterXref;

	private final QueryParameterBindingsImpl parameterBindings;

	private final QueryOptionsImpl queryOptions = new QueryOptionsImpl();
	private Callback callback;

	/**
	 * Creates a Query instance from a named HQL memento
	 */
	@SuppressWarnings("unchecked")
	public QuerySqmImpl(
			NamedHqlQueryMemento memento,
			Class<R> resultType,
			SharedSessionContractImplementor producer) {
		super( producer );

		this.hqlString = memento.getHqlString();

		final SessionFactoryImplementor factory = producer.getFactory();
		final QueryEngine queryEngine = factory.getQueryEngine();
		final QueryInterpretationCache interpretationCache = queryEngine.getInterpretationCache();
		final HqlInterpretation hqlInterpretation = interpretationCache.resolveHqlInterpretation(
				hqlString,
				s -> queryEngine.getHqlTranslator().translate( hqlString )
		);

		this.sqmStatement = hqlInterpretation.getSqmStatement();

		if ( resultType != null ) {
			if ( sqmStatement instanceof SqmDmlStatement ) {
				throw new IllegalArgumentException( "Non-select queries cannot be typed" );
			}
		}
		else if ( sqmStatement instanceof SqmUpdateStatement<?> ) {
			verifyImmutableEntityUpdate( hqlString, (SqmUpdateStatement<R>) sqmStatement, producer.getFactory() );
		}
		else if ( sqmStatement instanceof SqmInsertStatement<?> ) {
			verifyInsertTypesMatch( hqlString, (SqmInsertStatement<R>) sqmStatement );
		}
		this.resultType = resultType;
		this.domainParameterXref = hqlInterpretation.getDomainParameterXref();
		this.parameterMetadata = hqlInterpretation.getParameterMetadata();

		this.parameterBindings = QueryParameterBindingsImpl.from(
				parameterMetadata,
				producer.getFactory(),
				producer.isQueryParametersValidationEnabled()
		);

		applyOptions( memento );
	}

	protected void applyOptions(NamedHqlQueryMemento memento) {
		super.applyOptions( memento );

		if ( memento.getFirstResult() != null ) {
			setFirstResult( memento.getFirstResult() );
		}

		if ( memento.getMaxResults() != null ) {
			setMaxResults( memento.getMaxResults() );
		}

		if ( memento.getParameterTypes() != null ) {
			for ( Map.Entry<String, String> entry : memento.getParameterTypes().entrySet() ) {
				final QueryParameterImplementor<?> parameter = parameterMetadata.getQueryParameter( entry.getKey() );
				final BasicType<?> type = getSessionFactory().getTypeConfiguration()
						.getBasicTypeRegistry()
						.getRegisteredType( entry.getValue() );
				parameter.applyAnticipatedType( type );
			}
		}
	}

	/**
	 * Form used for HQL queries
	 */
	@SuppressWarnings("unchecked")
	public QuerySqmImpl(
			String hqlString,
			HqlInterpretation hqlInterpretation,
			Class<R> resultType,
			SharedSessionContractImplementor producer) {
		super( producer );

		this.hqlString = hqlString;
		this.resultType = resultType;

		this.sqmStatement = hqlInterpretation.getSqmStatement();

		if ( resultType != null ) {
			SqmUtil.verifyIsSelectStatement( sqmStatement, hqlString );
			visitQueryReturnType(
					( (SqmSelectStatement<R>) sqmStatement ).getQueryPart(),
					resultType,
					producer.getFactory()
			);
		}
		else if ( sqmStatement instanceof SqmUpdateStatement<?> ) {
			verifyImmutableEntityUpdate( hqlString, (SqmUpdateStatement<R>) sqmStatement, producer.getFactory() );
		}
		else if ( sqmStatement instanceof SqmInsertStatement<?> ) {
			verifyInsertTypesMatch( hqlString, (SqmInsertStatement<R>) sqmStatement );
		}

		this.parameterMetadata = hqlInterpretation.getParameterMetadata();
		this.domainParameterXref = hqlInterpretation.getDomainParameterXref();

		this.parameterBindings = QueryParameterBindingsImpl.from(
				parameterMetadata,
				producer.getFactory(),
				producer.isQueryParametersValidationEnabled()
		);
	}

	/**
	 * Form used for criteria queries
	 */
	@SuppressWarnings("unchecked")
	public QuerySqmImpl(
			SqmStatement<R> sqmStatement,
			Class<R> resultType,
			SharedSessionContractImplementor producer) {
		super( producer );

		if ( resultType != null ) {
			SqmUtil.verifyIsSelectStatement( sqmStatement, null );
			final SqmQueryPart<R> queryPart = ( (SqmSelectStatement<R>) sqmStatement ).getQueryPart();
			// For criteria queries, we have to validate the fetch structure here
			queryPart.validateQueryStructureAndFetchOwners();
			visitQueryReturnType(
					queryPart,
					resultType,
					producer.getFactory()
			);
		}
		else if ( sqmStatement instanceof SqmUpdateStatement<?> ) {
			final SqmUpdateStatement<R> updateStatement = (SqmUpdateStatement<R>) sqmStatement;
			verifyImmutableEntityUpdate( CRITERIA_HQL_STRING, updateStatement, producer.getFactory() );
			if ( updateStatement.getSetClause() == null || updateStatement.getSetClause().getAssignments().isEmpty() ) {
				throw new IllegalArgumentException( "No assignments specified as part of UPDATE criteria" );
			}
		}
		else if ( sqmStatement instanceof SqmInsertStatement<?> ) {
			verifyInsertTypesMatch( CRITERIA_HQL_STRING, (SqmInsertStatement<R>) sqmStatement );
		}

		this.hqlString = CRITERIA_HQL_STRING;
		this.sqmStatement = sqmStatement;
		this.resultType = resultType;

		this.domainParameterXref = DomainParameterXref.from( sqmStatement );
		if ( ! domainParameterXref.hasParameters() ) {
			this.parameterMetadata = ParameterMetadataImpl.EMPTY;
		}
		else {
			this.parameterMetadata = new ParameterMetadataImpl( domainParameterXref.getQueryParameters() );
		}

		this.parameterBindings = QueryParameterBindingsImpl.from(
				parameterMetadata,
				producer.getFactory(),
				producer.isQueryParametersValidationEnabled()
		);
		// Parameters might be created through HibernateCriteriaBuilder.value which we need to bind here
		for ( SqmParameter<?> sqmParameter : this.domainParameterXref.getParameterResolutions().getSqmParameters() ) {
			if ( sqmParameter instanceof SqmJpaCriteriaParameterWrapper<?> ) {
				final JpaCriteriaParameter<Object> jpaCriteriaParameter = ( (SqmJpaCriteriaParameterWrapper<Object>) sqmParameter ).getJpaCriteriaParameter();
				final Object value = jpaCriteriaParameter.getValue();
				// We don't set a null value, unless the type is also null which is the case when using HibernateCriteriaBuilder.value
				if ( value != null || jpaCriteriaParameter.getNodeType() == null ) {
					// Use the anticipated type for binding the value if possible
					getQueryParameterBindings().getBinding( jpaCriteriaParameter )
							.setBindValue( value, jpaCriteriaParameter.getAnticipatedType() );
				}
			}
		}
	}

	private void visitQueryReturnType(
			SqmQueryPart<R> queryPart,
			Class<R> resultType,
			SessionFactoryImplementor factory) {
		if ( queryPart instanceof SqmQuerySpec<?> ) {
			final SqmQuerySpec<R> sqmQuerySpec = (SqmQuerySpec<R>) queryPart;
			final List<SqmSelection<?>> sqmSelections = sqmQuerySpec.getSelectClause().getSelections();

			if ( sqmSelections == null || sqmSelections.isEmpty() ) {
				// make sure there is at least one root
				final List<SqmRoot<?>> sqmRoots = sqmQuerySpec.getFromClause().getRoots();
				if ( sqmRoots == null || sqmRoots.isEmpty() ) {
					throw new IllegalArgumentException( "Criteria did not define any query roots" );
				}
				// if there is a single root, use that as the selection
				if ( sqmRoots.size() == 1 ) {
					final SqmRoot<?> sqmRoot = sqmRoots.get( 0 );
					sqmQuerySpec.getSelectClause().add( sqmRoot, null );
				}
				else {
					throw new IllegalArgumentException(  );
				}
			}

			if ( resultType != null ) {
				checkQueryReturnType( sqmQuerySpec, resultType, factory );
			}
		}
		else {
			final SqmQueryGroup<R> queryGroup = (SqmQueryGroup<R>) queryPart;
			for ( SqmQueryPart<R> sqmQueryPart : queryGroup.getQueryParts() ) {
				visitQueryReturnType( sqmQueryPart, resultType, factory );
			}
		}
	}

	private static <T> void checkQueryReturnType(
			SqmQuerySpec<T> querySpec,
			Class<T> resultClass,
			SessionFactoryImplementor sessionFactory) {
		if ( resultClass == null ) {
			// nothing to check
			return;
		}

		final List<SqmSelection<?>> selections = querySpec.getSelectClause().getSelections();

		if ( resultClass.isArray() ) {
			// todo (6.0) : implement
		}
		else if ( Tuple.class.isAssignableFrom( resultClass ) ) {
			// todo (6.0) : implement
		}
		else {
			if ( selections.size() != 1 ) {
				final String errorMessage = "Query result-type error - multiple selections: use Tuple or array";

				if ( sessionFactory.getSessionFactoryOptions().getJpaCompliance().isJpaQueryComplianceEnabled() ) {
					throw new IllegalArgumentException( errorMessage );
				}
				else {
					throw new QueryTypeMismatchException( errorMessage );
				}
			}

			final SqmSelection<?> sqmSelection = selections.get( 0 );

			if ( sqmSelection.getSelectableNode() instanceof SqmParameter ) {
				final SqmParameter<?> sqmParameter = (SqmParameter<?>) sqmSelection.getSelectableNode();

				// we may not yet know a selection type
				if ( sqmParameter.getNodeType() == null || sqmParameter.getNodeType().getExpressableJavaType() == null ) {
					// we can't verify the result type up front
					return;
				}
			}

			verifyResultType( resultClass, sqmSelection.getNodeType(), sessionFactory );
		}
	}

	private static <T> void verifyResultType(
			Class<T> resultClass,
			SqmExpressable<?> sqmExpressable,
			SessionFactoryImplementor sessionFactory) {
		assert sqmExpressable != null;
		assert sqmExpressable.getExpressableJavaType() != null;

		final Class<?> javaTypeClass = sqmExpressable.getExpressableJavaType().getJavaTypeClass();
		if ( ! resultClass.isAssignableFrom( javaTypeClass ) ) {
			// Special case for date because we always report java.util.Date as expression type
			// But the expected resultClass could be a subtype of that, so we need to check the JdbcType
			if ( javaTypeClass == Date.class ) {
				JdbcType jdbcType = null;
				if ( sqmExpressable instanceof BasicDomainType<?> ) {
					jdbcType = ( (BasicDomainType<?>) sqmExpressable ).getJdbcType();
				}
				else if ( sqmExpressable instanceof SqmPathSource<?> ) {
					final DomainType<?> domainType = ( (SqmPathSource<?>) sqmExpressable ).getSqmPathType();
					if ( domainType instanceof BasicDomainType<?> ) {
						jdbcType = ( (BasicDomainType<?>) domainType ).getJdbcType();
					}
				}
				if ( jdbcType != null ) {
					switch ( jdbcType.getJdbcTypeCode() ) {
						case Types.DATE:
							if ( resultClass.isAssignableFrom( java.sql.Date.class ) ) {
								return;
							}
							break;
						case Types.TIME:
							if ( resultClass.isAssignableFrom( java.sql.Time.class ) ) {
								return;
							}
							break;
						case Types.TIMESTAMP:
							if ( resultClass.isAssignableFrom( java.sql.Timestamp.class ) ) {
								return;
							}
							break;
					}
				}
			}
			final String errorMessage = String.format(
					"Specified result type [%s] did not match Query selection type [%s] - multiple selections: use Tuple or array",
					resultClass.getName(),
					sqmExpressable.getExpressableJavaType().getJavaType().getTypeName()
			);

			if ( sessionFactory.getSessionFactoryOptions().getJpaCompliance().isJpaQueryComplianceEnabled() ) {
				throw new IllegalArgumentException( errorMessage );
			}
			else {
				throw new QueryTypeMismatchException( errorMessage );
			}
		}
	}

	private void verifyImmutableEntityUpdate(
			String hqlString,
			SqmUpdateStatement<R> sqmStatement,
			SessionFactoryImplementor factory) {
		final EntityPersister entityDescriptor = factory.getDomainModel()
				.getEntityDescriptor( sqmStatement.getTarget().getEntityName() );
		if ( entityDescriptor.isMutable() ) {
			return;
		}
		final ImmutableEntityUpdateQueryHandlingMode immutableEntityUpdateQueryHandlingMode = factory
				.getSessionFactoryOptions()
				.getImmutableEntityUpdateQueryHandlingMode();

		final String querySpaces = Arrays.toString( entityDescriptor.getQuerySpaces() );

		switch ( immutableEntityUpdateQueryHandlingMode ) {
			case WARNING:
				LOG.immutableEntityUpdateQuery( hqlString, querySpaces );
				break;
			case EXCEPTION:
				throw new HibernateException(
						"The query: [" + hqlString + "] attempts to update an immutable entity: " + querySpaces
				);
			default:
				throw new UnsupportedOperationException(
						"The " + immutableEntityUpdateQueryHandlingMode + " is not supported!"
				);
		}
	}

	private void verifyInsertTypesMatch(String hqlString, SqmInsertStatement<R> sqmStatement) {
		final List<SqmPath<?>> insertionTargetPaths = sqmStatement.getInsertionTargetPaths();
		if ( sqmStatement instanceof SqmInsertValuesStatement<?> ) {
			final SqmInsertValuesStatement<R> statement = (SqmInsertValuesStatement<R>) sqmStatement;
			for ( SqmValues sqmValues : statement.getValuesList() ) {
				verifyInsertTypesMatch( hqlString, insertionTargetPaths, sqmValues.getExpressions() );
			}
		}
		else {
			final SqmInsertSelectStatement<R> statement = (SqmInsertSelectStatement<R>) sqmStatement;
			final List<SqmSelection<?>> selections = statement.getSelectQueryPart()
					.getFirstQuerySpec()
					.getSelectClause()
					.getSelections();
			verifyInsertTypesMatch( hqlString, insertionTargetPaths, selections );
			statement.getSelectQueryPart().validateQueryStructureAndFetchOwners();
		}
	}

	private void verifyInsertTypesMatch(
			String hqlString,
			List<SqmPath<?>> insertionTargetPaths,
			List<? extends SqmTypedNode<?>> expressions) {
		final int size = insertionTargetPaths.size();
		final int expressionsSize = expressions.size();
		if ( size != expressionsSize ) {
			throw new SemanticException(
				String.format(
						"Expected insert attribute count [%d] did not match Query selection count [%d]",
						size,
						expressionsSize
				),
				hqlString,
				null
			);
		}
		for ( int i = 0; i < expressionsSize; i++ ) {
			final SqmTypedNode<?> expression = expressions.get( i );
			if ( expression.getNodeJavaType() == null ) {
				continue;
			}
			if ( insertionTargetPaths.get( i ).getJavaTypeDescriptor() != expression.getNodeJavaType() ) {
				throw new SemanticException(
						String.format(
								"Expected insert attribute type [%s] did not match Query selection type [%s] at selection index [%d]",
								insertionTargetPaths.get( i ).getJavaTypeDescriptor().getJavaType().getTypeName(),
								expression.getNodeJavaType().getJavaType().getTypeName(),
								i
						),
						hqlString,
						null
				);
			}
		}
	}


	public SessionFactoryImplementor getSessionFactory() {
		return getSession().getFactory();
	}

	@Override
	public QueryParameterBindings getParameterBindings() {
		return parameterBindings;
	}

	protected Boolean isSelectQuery() {
		return sqmStatement instanceof SqmSelectStatement;
	}

	@Override
	public String getQueryString() {
		return hqlString;
	}

	public DomainParameterXref getDomainParameterXref() {
		return domainParameterXref;
	}

	@Override
	public HqlQueryImplementor<R> applyGraph(@SuppressWarnings("rawtypes") RootGraph graph, GraphSemantic semantic) {
		queryOptions.applyGraph( (RootGraphImplementor<?>) graph, semantic );
		return this;
	}

	@Override
	protected void applyEntityGraphQueryHint(String hintName, @SuppressWarnings("rawtypes") RootGraphImplementor entityGraph) {
		final GraphSemantic graphSemantic = GraphSemantic.fromJpaHintName( hintName );

		applyGraph( entityGraph, graphSemantic );
	}

	@Override
	public SqmStatement<R> getSqmStatement() {
		return sqmStatement;
	}

	public Class<R> getResultType() {
		return resultType;
	}

	@Override
	public MutableQueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	public ParameterMetadataImplementor getParameterMetadata() {
		return parameterMetadata;
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return parameterBindings;
	}

	@Override
	public Set<Parameter<?>> getParameters() {
		getSession().checkOpen( false );
		Set<Parameter<?>> parameters = new HashSet<>();
		parameterMetadata.collectAllParameters( parameters::add );
		return parameters;
	}

	@Override
	protected boolean resolveJdbcParameterTypeIfNecessary() {
		// No need to resolve JDBC parameter types as we know them from the SQM model
		return false;
	}

	@Override
	public LockModeType getLockMode() {
		if ( ! isSelectQuery() ) {
			throw new IllegalStateException( "Illegal attempt to access lock-mode for non-select query" );
		}

		return super.getLockMode();
	}

	@Override
	public HqlQueryImplementor<R> setLockMode(LockModeType lockModeType) {
		if ( ! LockModeType.NONE.equals( lockModeType ) ) {
			if ( ! isSelectQuery() ) {
				throw new IllegalStateException( "Illegal attempt to access lock-mode for non-select query" );
			}
		}

		return (HqlQueryImplementor<R>) super.setLockMode( lockModeType );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( cls.isInstance( this ) ) {
			return (T) this;
		}

		if ( cls.isInstance( parameterMetadata ) ) {
			return (T) parameterMetadata;
		}

		if ( cls.isInstance( parameterBindings ) ) {
			return (T) parameterBindings;
		}

		if ( cls.isInstance( sqmStatement ) ) {
			return (T) sqmStatement;
		}

		if ( cls.isInstance( queryOptions ) ) {
			return (T) queryOptions;
		}

		if ( cls.isInstance( queryOptions.getAppliedGraph() ) ) {
			return (T) queryOptions.getAppliedGraph();
		}

		if ( EntityGraphQueryHint.class.isAssignableFrom( cls ) ) {
			return (T) new EntityGraphQueryHint( queryOptions.getAppliedGraph() );
		}

		throw new PersistenceException( "Unrecognized unwrap type [" + cls.getName() + "]" );
	}

	@Override
	protected boolean applyNativeQueryLockMode(Object value) {
		throw new IllegalStateException(
				"Illegal attempt to set lock mode on non-native query via hint; use Query#setLockMode instead"
		);
	}

	@Override
	protected boolean applySynchronizeSpacesHint(Object value) {
		throw new IllegalStateException(
				"Illegal attempt to set synchronized spaces on non-native query via hint"
		);
	}

	@Override
	protected void collectHints(Map<String, Object> hints) {
		super.collectHints( hints );

		if ( queryOptions.getAppliedGraph() != null && queryOptions.getAppliedGraph().getSemantic() != null ) {
			hints.put(
					queryOptions.getAppliedGraph().getSemantic().getJpaHintName(),
					queryOptions.getAppliedGraph().getGraph()
			);
		}
	}

	@Override
	protected List<R> doList() {
		SqmUtil.verifyIsSelectStatement( getSqmStatement(), hqlString );
		final SqmSelectStatement<?> selectStatement = (SqmSelectStatement<?>) getSqmStatement();

		getSession().prepareForQueryExecution( requiresTxn( getLockOptions().findGreatestLockMode() ) );
		final boolean containsCollectionFetches = selectStatement.containsCollectionFetches();
		final boolean hasLimit = queryOptions.hasLimit() || selectStatement.getFetch() != null || selectStatement.getOffset() != null;
		final boolean needsDistincting = containsCollectionFetches && (
				selectStatement.usesDistinct() ||
						queryOptions.getGraph() != null ||
						hasLimit
		);

		final DomainQueryExecutionContext executionContextToUse;
		if ( hasLimit && containsCollectionFetches ) {
			boolean fail = getSessionFactory().getSessionFactoryOptions().isFailOnPaginationOverCollectionFetchEnabled();
			if (fail) {
				throw new HibernateException(
						"firstResult/maxResults specified with collection fetch. " +
								"In memory pagination was about to be applied. " +
								"Failing because 'Fail on pagination over collection fetch' is enabled."
				);
			}
			else {
				LOG.firstOrMaxResultsSpecifiedWithCollectionFetch();
			}

			final MutableQueryOptions originalQueryOptions = getQueryOptions();
			final QueryOptions normalizedQueryOptions = omitSqlQueryOptions( originalQueryOptions, true, false );
			if ( originalQueryOptions == normalizedQueryOptions ) {
				executionContextToUse = this;
			}
			else {
				executionContextToUse = new DelegatingDomainQueryExecutionContext( this ) {
					@Override
					public QueryOptions getQueryOptions() {
						return normalizedQueryOptions;
					}
				};
			}
		}
		else {
			executionContextToUse = this;
		}

		final List<R> list = resolveSelectQueryPlan().performList( executionContextToUse );

		if ( needsDistincting ) {
			int includedCount = -1;
			// NOTE : firstRow is zero-based
			final int first = !hasLimit || queryOptions.getLimit().getFirstRow() == null
					? getIntegerLiteral( selectStatement.getOffset(), 0 )
					: queryOptions.getLimit().getFirstRow();
			final int max = !hasLimit || queryOptions.getLimit().getMaxRows() == null
					? getMaxRows( selectStatement, list.size() )
					: queryOptions.getLimit().getMaxRows();
			final List<R> tmp = new ArrayList<>( list.size() );
			final IdentitySet<R> distinction = new IdentitySet<>( list.size() );
			for ( final R result : list ) {
				if ( !distinction.add( result ) ) {
					continue;
				}
				includedCount++;
				if ( includedCount < first ) {
					continue;
				}
				tmp.add( result );
				// NOTE : ( max - 1 ) because first is zero-based while max is not...
				if ( max >= 0 && ( includedCount - first ) >= ( max - 1 ) ) {
					break;
				}
			}
			return tmp;
		}
		return list;
	}

	private int getMaxRows(SqmSelectStatement<?> selectStatement, int size) {
		final JpaExpression<Number> expression = selectStatement.getFetch();
		if ( expression == null ) {
			return -1;
		}

		final Number fetchValue;
		if ( expression instanceof SqmLiteral<?> ) {
			fetchValue = ( (SqmLiteral<Number>) expression ).getLiteralValue();
		}
		else if ( expression instanceof SqmParameter<?> ) {
			fetchValue = getParameterValue( (Parameter<Number>) expression );
			if ( fetchValue == null ) {
				return -1;
			}
		}
		else {
			throw new IllegalArgumentException( "Can't get max rows value from: " + expression );
		}
		// Note that we can never have ties because this is only used when we de-duplicate results
		switch ( selectStatement.getFetchClauseType() ) {
			case ROWS_ONLY:
			case ROWS_WITH_TIES:
				return fetchValue.intValue();
			case PERCENT_ONLY:
			case PERCENT_WITH_TIES:
				return (int) Math.ceil( ( ( (double) size ) * fetchValue.doubleValue() ) / 100d );
		}
		throw new UnsupportedOperationException( "Unsupported fetch clause type: " + selectStatement.getFetchClauseType() );
	}

	private int getIntegerLiteral(JpaExpression<Number> expression, int defaultValue) {
		if ( expression == null ) {
			return defaultValue;
		}

		if ( expression instanceof SqmLiteral<?> ) {
			return ( (SqmLiteral<Number>) expression ).getLiteralValue().intValue();
		}
		else if ( expression instanceof SqmParameter<?> ) {
			final Number parameterValue = getParameterValue( (Parameter<Number>) expression );
			return parameterValue == null ? defaultValue : parameterValue.intValue();
		}
		throw new IllegalArgumentException( "Can't get integer literal value from: " + expression );
	}

	private boolean requiresTxn(LockMode lockMode) {
		return lockMode != null && lockMode.greaterThan( LockMode.READ );
	}

	private SelectQueryPlan<R> resolveSelectQueryPlan() {
		// resolve (or make) the QueryPlan.  This QueryPlan might be an aggregation of multiple plans.
		//
		// QueryPlans can be cached, except for in certain circumstances
		// 		- the determination of these circumstances occurs in SqmInterpretationsKey#generateFrom.
		//		If SqmInterpretationsKey#generateFrom returns null the query is not cacheable

		final QueryInterpretationCache.Key cacheKey = SqmInterpretationsKey.generateFrom( this );
		if ( cacheKey != null ) {
			return getSession().getFactory().getQueryEngine().getInterpretationCache().resolveSelectQueryPlan(
					cacheKey,
					this::buildSelectQueryPlan
			);
		}
		else {
			return buildSelectQueryPlan();
		}
	}

	private SelectQueryPlan<R> buildSelectQueryPlan() {
		final SqmSelectStatement<R>[] concreteSqmStatements = QuerySplitter.split(
				(SqmSelectStatement<R>) getSqmStatement(),
				getSessionFactory()
		);

		if ( concreteSqmStatements.length > 1 ) {
			return buildAggregatedSelectQueryPlan( concreteSqmStatements );
		}
		else {
			return buildConcreteSelectQueryPlan(
					concreteSqmStatements[0],
					getResultType(),
					getQueryOptions()
			);
		}
	}

	private SelectQueryPlan<R> buildAggregatedSelectQueryPlan(SqmSelectStatement<R>[] concreteSqmStatements) {
		@SuppressWarnings("unchecked")
		final SelectQueryPlan<R>[] aggregatedQueryPlans = new SelectQueryPlan[ concreteSqmStatements.length ];

		// todo (6.0) : we want to make sure that certain thing (ResultListTransformer, etc) only get applied at the aggregator-level

		for ( int i = 0, x = concreteSqmStatements.length; i < x; i++ ) {
			aggregatedQueryPlans[i] = buildConcreteSelectQueryPlan(
					concreteSqmStatements[i],
					getResultType(),
					getQueryOptions()
			);
		}

		return new AggregatedSelectQueryPlanImpl<>( aggregatedQueryPlans );
	}

	private SelectQueryPlan<R> buildConcreteSelectQueryPlan(
			SqmSelectStatement<R> concreteSqmStatement,
			Class<R> resultType,
			QueryOptions queryOptions) {
		return new ConcreteSqmSelectQueryPlan<>(
				concreteSqmStatement,
				hqlString,
				domainParameterXref,
				resultType,
				queryOptions
		);
	}

	@Override
	public ScrollableResultsImplementor<R> scroll(ScrollMode scrollMode) {
		SqmUtil.verifyIsSelectStatement( getSqmStatement(), hqlString );
		getSession().prepareForQueryExecution( requiresTxn( getLockOptions().findGreatestLockMode() ) );

		return resolveSelectQueryPlan().performScroll( scrollMode, this );
	}

	@Override
	protected int doExecuteUpdate() {
		SqmUtil.verifyIsNonSelectStatement( getSqmStatement(), hqlString );
		getSession().prepareForQueryExecution( true );

		return resolveNonSelectQueryPlan().executeUpdate( this );
	}

	private NonSelectQueryPlan resolveNonSelectQueryPlan() {
		// resolve (or make) the QueryPlan.

		NonSelectQueryPlan queryPlan = null;

		final QueryInterpretationCache.Key cacheKey = SqmInterpretationsKey.generateNonSelectKey( this );
		if ( cacheKey != null ) {
			queryPlan = getSession().getFactory().getQueryEngine().getInterpretationCache().getNonSelectQueryPlan( cacheKey );
		}

		if ( queryPlan == null ) {
			queryPlan = buildNonSelectQueryPlan();
			if ( cacheKey != null ) {
				getSession().getFactory().getQueryEngine().getInterpretationCache().cacheNonSelectQueryPlan( cacheKey, queryPlan );
			}
		}

		return queryPlan;
	}

	private NonSelectQueryPlan buildNonSelectQueryPlan() {
		// to get here the SQM statement has already been validated to be
		// a non-select variety...
		if ( getSqmStatement() instanceof SqmDeleteStatement<?> ) {
			return buildDeleteQueryPlan();
		}

		if ( getSqmStatement() instanceof SqmUpdateStatement<?> ) {
			return buildUpdateQueryPlan();
		}

		if ( getSqmStatement() instanceof SqmInsertStatement<?> ) {
			return buildInsertQueryPlan();
		}

		throw new NotYetImplementedException( "Query#executeUpdate for Statements of type [" + getSqmStatement() + "not yet supported" );
	}

	private NonSelectQueryPlan buildDeleteQueryPlan() {
		final SqmDeleteStatement<R>[] concreteSqmStatements = QuerySplitter.split(
				(SqmDeleteStatement<R>) getSqmStatement(),
				getSessionFactory()
		);

		if ( concreteSqmStatements.length > 1 ) {
			return buildAggregatedDeleteQueryPlan( concreteSqmStatements );
		}
		else {
			return buildConcreteDeleteQueryPlan( concreteSqmStatements[0] );
		}
	}

	private NonSelectQueryPlan buildConcreteDeleteQueryPlan(SqmDeleteStatement<R> sqmDelete) {
		final EntityDomainType<?> entityDomainType = sqmDelete.getTarget().getReferencedPathSource();
		final String entityNameToDelete = entityDomainType.getHibernateEntityName();
		final EntityPersister entityDescriptor = getSessionFactory().getDomainModel().findEntityDescriptor( entityNameToDelete );
		final SqmMultiTableMutationStrategy multiTableStrategy = entityDescriptor.getSqmMultiTableMutationStrategy();
		if ( multiTableStrategy == null ) {
			return new SimpleDeleteQueryPlan( entityDescriptor, sqmDelete, domainParameterXref );
		}
		else {
			return new MultiTableDeleteQueryPlan( sqmDelete, domainParameterXref, multiTableStrategy );
		}
	}

	private NonSelectQueryPlan buildAggregatedDeleteQueryPlan(SqmDeleteStatement<R>[] concreteSqmStatements) {
		final NonSelectQueryPlan[] aggregatedQueryPlans = new NonSelectQueryPlan[ concreteSqmStatements.length ];

		for ( int i = 0, x = concreteSqmStatements.length; i < x; i++ ) {
			aggregatedQueryPlans[i] = buildConcreteDeleteQueryPlan( concreteSqmStatements[i] );
		}

		return new AggregatedNonSelectQueryPlanImpl( aggregatedQueryPlans );
	}

	private NonSelectQueryPlan buildUpdateQueryPlan() {
		final SqmUpdateStatement<R> sqmUpdate = (SqmUpdateStatement<R>) getSqmStatement();

		final String entityNameToUpdate = sqmUpdate.getTarget().getReferencedPathSource().getHibernateEntityName();
		final EntityPersister entityDescriptor = getSessionFactory().getDomainModel().findEntityDescriptor( entityNameToUpdate );

		final SqmMultiTableMutationStrategy multiTableStrategy = entityDescriptor.getSqmMultiTableMutationStrategy();
		if ( multiTableStrategy == null ) {
			return new SimpleUpdateQueryPlan( sqmUpdate, domainParameterXref );
		}
		else {
			return new MultiTableUpdateQueryPlan( sqmUpdate, domainParameterXref, multiTableStrategy );
		}
	}

	private NonSelectQueryPlan buildInsertQueryPlan() {
		final SqmInsertStatement<R> sqmInsert = (SqmInsertStatement<R>) getSqmStatement();

		final String entityNameToInsert = sqmInsert.getTarget().getReferencedPathSource().getHibernateEntityName();
		final EntityPersister entityDescriptor = getSessionFactory().getDomainModel().findEntityDescriptor( entityNameToInsert );

		final SqmMultiTableInsertStrategy multiTableStrategy = entityDescriptor.getSqmMultiTableInsertStrategy();
		if ( multiTableStrategy == null || isSimpleValuesInsert( sqmInsert, entityDescriptor ) ) {
			return new SimpleInsertQueryPlan( sqmInsert, domainParameterXref );
		}
		else {
			return new MultiTableInsertQueryPlan( sqmInsert, domainParameterXref, multiTableStrategy );
		}
	}

	private boolean isSimpleValuesInsert(SqmInsertStatement<R> sqmInsert, EntityPersister entityDescriptor) {
		// Simple means that we can translate the statement to a single plain insert
		return sqmInsert instanceof SqmInsertValuesStatement
				// An insert is only simple if no SqmMultiTableMutation strategy is available,
				// as the presence of it means the entity has multiple tables involved,
				// in which case we currently need to use the MultiTableInsertQueryPlan
				&& entityDescriptor.getSqmMultiTableMutationStrategy() == null;
	}

	@Override
	protected void prepareForExecution() {
		super.prepareForExecution();
		// Reset the callback before every execution
		callback = null;
	}

	@Override
	public Callback getCallback() {
		if ( callback == null ) {
			callback = new CallbackImpl();
		}
		return callback;
	}

	@Override
	public NamedHqlQueryMemento toMemento(String name) {
		return new NamedHqlQueryMementoImpl(
				name,
				hqlString,
				getFirstResult(),
				getMaxResults(),
				isCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getHibernateFlushMode(),
				isReadOnly(),
				getLockOptions(),
				getTimeout(),
				getFetchSize(),
				getComment(),
				Collections.emptyMap(),
				getHints()
		);
	}

}
