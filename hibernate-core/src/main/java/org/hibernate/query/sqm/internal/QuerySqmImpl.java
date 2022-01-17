/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.io.Serializable;
import java.sql.Types;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.BindableType;
import org.hibernate.query.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.SemanticException;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.hql.internal.NamedHqlQueryMementoImpl;
import org.hibernate.query.hql.spi.NamedHqlQueryMemento;
import org.hibernate.query.hql.spi.SqmQueryImplementor;
import org.hibernate.query.spi.AbstractSqmQuery;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.HqlInterpretation;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.domain.SqmPath;
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
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Tuple;

/**
 * {@link Query} implementation based on an SQM
 *
 * @author Steve Ebersole
 */
public class QuerySqmImpl<R>
		extends AbstractSqmQuery
		implements SqmQueryImplementor<R>, DomainQueryExecutionContext {

	/**
	 * The value used for {@link #getQueryString} for Criteria-based queries
	 */
	public static final String CRITERIA_HQL_STRING = "<criteria>";
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( QuerySqmImpl.class );

	private final Class<R> resultType;

	/**
	 * Creates a Query instance from a named HQL memento
	 */
	public QuerySqmImpl(
			NamedHqlQueryMemento memento,
			Class<R> resultType,
			SharedSessionContractImplementor producer) {
		super(
				memento.getHqlString(),
				producer.getFactory().getQueryEngine().getInterpretationCache().resolveHqlInterpretation(
						memento.getHqlString(),
						s -> producer.getFactory().getQueryEngine().getHqlTranslator().translate( memento.getHqlString() )
				),
				producer
		);

		this.resultType = resultType;

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
				final QueryParameterImplementor<?> parameter = getParameterMetadata().getQueryParameter( entry.getKey() );
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
		super( hqlString, hqlInterpretation, producer );

		this.resultType = resultType;

		//noinspection rawtypes
		final SqmStatement sqmStatement = hqlInterpretation.getSqmStatement();
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
	}

	/**
	 * Form used for criteria queries
	 */
	public QuerySqmImpl(
			SqmStatement<R> sqmStatement,
			Class<R> resultType,
			SharedSessionContractImplementor producer) {
		super( sqmStatement, producer );

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

		this.resultType = resultType;
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
		return getQueryParameterBindings();
	}

	public Class<R> getResultType() {
		return resultType;
	}

	@Override
	public SqmQueryImplementor<R> addQueryHint(String hint) {
		getQueryOptions().addDatabaseHint( hint );
		return this;
	}

	@Override
	public LockOptions getLockOptions() {
		return getQueryOptions().getLockOptions();
	}

	@Override
	public SqmQueryImplementor<R> setLockOptions(LockOptions lockOptions) {
		verifySelect();
		applyLockOptions( lockOptions );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setLockMode(String alias, LockMode lockMode) {
		verifySelect();
		applyLockMode( alias, lockMode );
		return this;
	}

	@Override
	public <T> SqmQueryImplementor<T> setTupleTransformer(TupleTransformer<T> transformer) {
		applyTupleTransformer( transformer );
		//noinspection unchecked
		return (SqmQueryImplementor<T>) this;
	}

	@Override
	public SqmQueryImplementor<R> setResultListTransformer(ResultListTransformer transformer) {
		applyResultListTransformer( transformer );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setMaxResults(int maxResult) {
		applyMaxResults( maxResult );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setFirstResult(int startPosition) {
		applyFirstResult( startPosition );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setHint(String hintName, Object value) {
		applyHint( hintName, value );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setHibernateFlushMode(FlushMode flushMode) {
		super.setHibernateFlushMode( flushMode );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setFlushMode(FlushModeType flushMode) {
		applyJpaFlushMode( flushMode );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setLockMode(LockModeType lockMode) {
		if ( lockMode != LockModeType.NONE ) {
			verifySelect();
		}
		applyJpaLockMode( lockMode );
		return this;
	}

	@Override
	public LockModeType getLockMode() {
		verifySelect();
		return getJpaLockMode();
	}

	@Override
	public FlushModeType getFlushMode() {
		return getJpaFlushMode();
	}

	@Override
	public SqmQueryImplementor<R> applyGraph(RootGraph graph, GraphSemantic semantic) {
		super.applyGraph( (RootGraphImplementor<?>) graph, semantic );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// execution

	@Override
	public List<R> list() {
		//noinspection unchecked
		return super.list();
	}

	@Override
	public ScrollableResultsImplementor<R> scroll() {
		//noinspection unchecked
		return super.scroll();
	}

	@Override
	public ScrollableResultsImplementor<R> scroll(ScrollMode scrollMode) {
		//noinspection unchecked
		return super.scroll( scrollMode );
	}

	@Override
	public Stream<R> stream() {
		//noinspection unchecked
		return super.stream();
	}

	@Override
	public R uniqueResult() {
		//noinspection unchecked
		return (R) super.uniqueResult();
	}

	@Override
	public R getSingleResult() {
		//noinspection unchecked
		return (R) super.getSingleResult();
	}

	@Override
	public Optional<R> uniqueResultOptional() {
		//noinspection unchecked
		return super.uniqueResultOptional();
	}

	@Override
	public int executeUpdate() {
		return super.executeUpdate();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Named query externalization

	@Override
	public NamedHqlQueryMemento toMemento(String name) {
		if ( CRITERIA_HQL_STRING.equals( getQueryString() ) ) {
			throw new UnsupportedOperationException( "Criteria-based Query cannot be saved as a named query" );
		}

		return new NamedHqlQueryMementoImpl(
				name,
				getQueryString(),
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariance


	@Override
	public SqmQueryImplementor<R> setComment(String comment) {
		super.setComment( comment );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setCacheMode(CacheMode cacheMode) {
		super.setCacheMode( cacheMode );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setCacheable(boolean cacheable) {
		super.setCacheable( cacheable );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setCacheRegion(String cacheRegion) {
		super.setCacheRegion( cacheRegion );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setFetchSize(int fetchSize) {
		super.setFetchSize( fetchSize );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setReadOnly(boolean readOnly) {
		super.setReadOnly( readOnly );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setProperties(Object bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setProperties(Map bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(String name, P value, Class<P> javaType) {
		super.setParameter( name, value, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(String name, P value, BindableType<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(String name, Instant value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(int position, P value, Class<P> javaType) {
		super.setParameter( position, value, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(int position, P value, BindableType<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(int position, Instant value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, Class<P> javaType) {
		super.setParameter( parameter, value, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(QueryParameter<P> parameter, P value, BindableType<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameter(Parameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameter(int position, Date value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameterList(String name, Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(String name, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameterList(String name, Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(String name, P[] values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(String name, P[] values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameterList(int position, Collection values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(int position, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public SqmQueryImplementor<R> setParameterList(int position, Object[] values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(int position, P[] values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(int position, P[] values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SqmQueryImplementor<R> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// optional object loading

	@Override
	public void setOptionalId(Serializable id) {
		throw new UnsupportedOperationException( "Not sure yet how to handle this in SQM based queries, but for sure it will be different" );
	}

	@Override
	public void setOptionalEntityName(String entityName) {
		throw new UnsupportedOperationException( "Not sure yet how to handle this in SQM based queries, but for sure it will be different" );
	}

	@Override
	public void setOptionalObject(Object optionalObject) {
		throw new UnsupportedOperationException( "Not sure yet how to handle this in SQM based queries, but for sure it will be different" );
	}
}
