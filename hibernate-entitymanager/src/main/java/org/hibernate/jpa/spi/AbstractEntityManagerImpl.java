/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.spi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceException;
import javax.persistence.PessimisticLockException;
import javax.persistence.PessimisticLockScope;
import javax.persistence.Query;
import javax.persistence.QueryTimeoutException;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.SynchronizationType;
import javax.persistence.TransactionRequiredException;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.AssertionFailure;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.QueryException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.TypeMismatchException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.lock.LockingStrategyException;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryConstructorReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.spi.JoinStatus;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.engine.transaction.synchronization.spi.AfterCompletionAction;
import org.hibernate.engine.transaction.synchronization.spi.ExceptionMapper;
import org.hibernate.engine.transaction.synchronization.spi.ManagedFlushChecker;
import org.hibernate.engine.transaction.synchronization.spi.SynchronizationCallbackCoordinator;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.criteria.ValueHandlerFactory;
import org.hibernate.jpa.criteria.compile.CompilableCriteria;
import org.hibernate.jpa.criteria.compile.CriteriaCompiler;
import org.hibernate.jpa.criteria.expression.CompoundSelectionImpl;
import org.hibernate.jpa.internal.EntityManagerFactoryImpl;
import org.hibernate.jpa.internal.EntityManagerMessageLogger;
import org.hibernate.jpa.internal.HEMLogging;
import org.hibernate.jpa.internal.QueryImpl;
import org.hibernate.jpa.internal.StoredProcedureQueryImpl;
import org.hibernate.jpa.internal.TransactionImpl;
import org.hibernate.jpa.internal.util.CacheModeHelper;
import org.hibernate.jpa.internal.util.ConfigurationHelper;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.UnknownSqlResultSetMappingException;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.transform.BasicTransformerAdapter;
import org.hibernate.type.Type;

/**
 * @author <a href="mailto:gavin@hibernate.org">Gavin King</a>
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public abstract class AbstractEntityManagerImpl implements HibernateEntityManagerImplementor, Serializable {
	private static final long serialVersionUID = 78818181L;

    private static final EntityManagerMessageLogger LOG = HEMLogging.messageLogger( AbstractEntityManagerImpl.class );

	private static final List<String> ENTITY_MANAGER_SPECIFIC_PROPERTIES = new ArrayList<String>();

	static {
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( AvailableSettings.LOCK_SCOPE );
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( AvailableSettings.LOCK_TIMEOUT );
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( AvailableSettings.FLUSH_MODE );
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE );
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( AvailableSettings.SHARED_CACHE_STORE_MODE );
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( QueryHints.SPEC_HINT_TIMEOUT );
	}

	private EntityManagerFactoryImpl entityManagerFactory;
	protected transient TransactionImpl tx = new TransactionImpl( this );
	private SynchronizationType synchronizationType;
	private PersistenceUnitTransactionType transactionType;
	private Map<String, Object> properties;
	private LockOptions lockOptions;

	protected AbstractEntityManagerImpl(
			EntityManagerFactoryImpl entityManagerFactory,
			PersistenceContextType type,  // TODO:  remove as no longer used
			SynchronizationType synchronizationType,
			PersistenceUnitTransactionType transactionType,
			Map properties) {
		this.entityManagerFactory = entityManagerFactory;
		this.synchronizationType = synchronizationType;
		this.transactionType = transactionType;

		this.lockOptions = new LockOptions();
		this.properties = new HashMap<String, Object>();
		for ( String key : ENTITY_MANAGER_SPECIFIC_PROPERTIES ) {
			if ( entityManagerFactory.getProperties().containsKey( key ) ) {
				this.properties.put( key, entityManagerFactory.getProperties().get( key ) );
			}
			if ( properties != null && properties.containsKey( key ) ) {
				this.properties.put( key, properties.get( key ) );
			}
		}
	}

//	protected PersistenceUnitTransactionType transactionType() {
//		return transactionType;
//	}
//
//	protected SynchronizationType synchronizationType() {
//		return synchronizationType;
//	}
//
//	public boolean shouldAutoJoinTransactions() {
//		// the Session should auto join only if using non-JTA transactions or if the synchronization type
//		// was specified as SYNCHRONIZED
//		return transactionType != PersistenceUnitTransactionType.JTA
//				|| synchronizationType == SynchronizationType.SYNCHRONIZED;
//	}

	public PersistenceUnitTransactionType getTransactionType() {
		return transactionType;
	}

	protected void postInit() {
		//register in Sync if needed
		if ( transactionType == PersistenceUnitTransactionType.JTA
				&& synchronizationType == SynchronizationType.SYNCHRONIZED ) {
			joinTransaction( false );
		}

		setDefaultProperties();
		applyProperties();
	}

	private void applyProperties() {
		getSession().setFlushMode( ConfigurationHelper.getFlushMode( properties.get( AvailableSettings.FLUSH_MODE ) ) );
		setLockOptions( this.properties, this.lockOptions );
		getSession().setCacheMode(
				CacheModeHelper.interpretCacheMode(
						currentCacheStoreMode(),
						currentCacheRetrieveMode()
				)
		);
	}

	private Query applyProperties(Query query) {
		if ( lockOptions.getLockMode() != LockMode.NONE ) {
			query.setLockMode( getLockMode(lockOptions.getLockMode()));
		}
		Object queryTimeout;
		if ( (queryTimeout = getProperties().get(QueryHints.SPEC_HINT_TIMEOUT)) != null ) {
			query.setHint( QueryHints.SPEC_HINT_TIMEOUT, queryTimeout );
		}
		Object lockTimeout;
		if( (lockTimeout = getProperties().get( AvailableSettings.LOCK_TIMEOUT ))!=null){
			query.setHint( AvailableSettings.LOCK_TIMEOUT, lockTimeout );
		}
		return query;
	}

	private CacheRetrieveMode currentCacheRetrieveMode() {
		return determineCacheRetrieveMode( properties );
	}

	private CacheRetrieveMode determineCacheRetrieveMode(Map<String, Object> settings) {
		return ( CacheRetrieveMode ) settings.get( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE );
	}

	private CacheStoreMode currentCacheStoreMode() {
		return determineCacheStoreMode( properties );
	}

	private CacheStoreMode determineCacheStoreMode(Map<String, Object> settings) {
		return ( CacheStoreMode ) settings.get( AvailableSettings.SHARED_CACHE_STORE_MODE );
	}

	private void setLockOptions(Map<String, Object> props, LockOptions options) {
		Object lockScope = props.get( AvailableSettings.LOCK_SCOPE );
		if ( lockScope instanceof String && PessimisticLockScope.valueOf( ( String ) lockScope ) == PessimisticLockScope.EXTENDED ) {
			options.setScope( true );
		}
		else if ( lockScope instanceof PessimisticLockScope ) {
			boolean extended = PessimisticLockScope.EXTENDED.equals( lockScope );
			options.setScope( extended );
		}
		else if ( lockScope != null ) {
			throw new PersistenceException( "Unable to parse " + AvailableSettings.LOCK_SCOPE + ": " + lockScope );
		}

		Object lockTimeout = props.get( AvailableSettings.LOCK_TIMEOUT );
		int timeout = 0;
		boolean timeoutSet = false;
		if ( lockTimeout instanceof String ) {
			timeout = Integer.parseInt( ( String ) lockTimeout );
			timeoutSet = true;
		}
		else if ( lockTimeout instanceof Number ) {
			timeout = ( (Number) lockTimeout ).intValue();
			timeoutSet = true;
		}
		else if ( lockTimeout != null ) {
			throw new PersistenceException( "Unable to parse " + AvailableSettings.LOCK_TIMEOUT + ": " + lockTimeout );
		}
		if ( timeoutSet ) {
            if ( timeout == LockOptions.SKIP_LOCKED ) {
                options.setTimeOut( LockOptions.SKIP_LOCKED );
            }
			else if ( timeout < 0 ) {
				options.setTimeOut( LockOptions.WAIT_FOREVER );
			}
			else if ( timeout == 0 ) {
				options.setTimeOut( LockOptions.NO_WAIT );
			}
			else {
				options.setTimeOut( timeout );
			}
		}
	}

	/**
	 * Sets the default property values for the properties the entity manager supports and which are not already explicitly
	 * set.
	 */
	private void setDefaultProperties() {
		if ( properties.get( AvailableSettings.FLUSH_MODE ) == null ) {
			properties.put( AvailableSettings.FLUSH_MODE, getSession().getFlushMode().toString() );
		}
		if ( properties.get( AvailableSettings.LOCK_SCOPE ) == null ) {
			this.properties.put( AvailableSettings.LOCK_SCOPE, PessimisticLockScope.EXTENDED.name() );
		}
		if ( properties.get( AvailableSettings.LOCK_TIMEOUT ) == null ) {
			properties.put( AvailableSettings.LOCK_TIMEOUT, LockOptions.WAIT_FOREVER );
		}
		if ( properties.get( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE ) == null ) {
			properties.put( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE, CacheModeHelper.DEFAULT_RETRIEVE_MODE );
		}
		if ( properties.get( AvailableSettings.SHARED_CACHE_STORE_MODE ) == null ) {
			properties.put( AvailableSettings.SHARED_CACHE_STORE_MODE, CacheModeHelper.DEFAULT_STORE_MODE );
		}
	}

	@Override
	public Query createQuery(String jpaqlString) {
		checkOpen();
		try {
			return applyProperties( new QueryImpl<Object>( internalGetSession().createQuery( jpaqlString ), this ) );
		}
		catch ( RuntimeException e ) {
			throw convert( e );
		}
	}

	protected abstract void checkOpen();

	@Override
	public <T> TypedQuery<T> createQuery(String jpaqlString, Class<T> resultClass) {
		checkOpen();
		try {
			// do the translation
			org.hibernate.Query hqlQuery = internalGetSession().createQuery( jpaqlString );

			resultClassChecking( resultClass, hqlQuery );

			// finally, build/return the query instance
			return new QueryImpl<T>( hqlQuery, this );
		}
		catch ( RuntimeException e ) {
			throw convert( e );
		}
	}

	protected void resultClassChecking(Class resultClass, org.hibernate.Query hqlQuery) {
		// make sure the query is a select -> HHH-7192
		final SessionImplementor session = unwrap( SessionImplementor.class );
		final HQLQueryPlan queryPlan = session.getFactory().getQueryPlanCache().getHQLQueryPlan(
				hqlQuery.getQueryString(),
				false,
				session.getLoadQueryInfluencers().getEnabledFilters()
		);
		if ( queryPlan.getTranslators()[0].isManipulationStatement() ) {
			throw new IllegalArgumentException( "Update/delete queries cannot be typed" );
		}

		// do some return type validation checking
		if ( Object[].class.equals( resultClass ) ) {
			// no validation needed
		}
		else if ( Tuple.class.equals( resultClass ) ) {
			TupleBuilderTransformer tupleTransformer = new TupleBuilderTransformer( hqlQuery );
			hqlQuery.setResultTransformer( tupleTransformer  );
		}
		else {
			final Class dynamicInstantiationClass = queryPlan.getDynamicInstantiationResultType();
			if ( dynamicInstantiationClass != null ) {
				if ( ! resultClass.isAssignableFrom( dynamicInstantiationClass ) ) {
					throw new IllegalArgumentException(
							"Mismatch in requested result type [" + resultClass.getName() +
									"] and actual result type [" + dynamicInstantiationClass.getName() + "]"
					);
				}
			}
			else if ( hqlQuery.getReturnTypes().length == 1 ) {
				// if we have only a single return expression, its java type should match with the requested type
				if ( !resultClass.isAssignableFrom( hqlQuery.getReturnTypes()[0].getReturnedClass() ) ) {
					throw new IllegalArgumentException(
							"Type specified for TypedQuery [" +
									resultClass.getName() +
									"] is incompatible with query return type [" +
									hqlQuery.getReturnTypes()[0].getReturnedClass() + "]"
					);
				}
			}
			else {
				throw new IllegalArgumentException(
						"Cannot create TypedQuery for query with more than one return using requested result type [" +
								resultClass.getName() + "]"
				);
			}
		}
	}

	public static class TupleBuilderTransformer extends BasicTransformerAdapter {
		private List<TupleElement<?>> tupleElements;
		private Map<String,HqlTupleElementImpl> tupleElementsByAlias;

		public TupleBuilderTransformer(org.hibernate.Query hqlQuery) {
			final Type[] resultTypes = hqlQuery.getReturnTypes();
			final int tupleSize = resultTypes.length;

			this.tupleElements = CollectionHelper.arrayList( tupleSize );

			final String[] aliases = hqlQuery.getReturnAliases();
			final boolean hasAliases = aliases != null && aliases.length > 0;
			this.tupleElementsByAlias = hasAliases
					? CollectionHelper.<String, HqlTupleElementImpl>mapOfSize( tupleSize )
					: Collections.<String, HqlTupleElementImpl>emptyMap();

			for ( int i = 0; i < tupleSize; i++ ) {
				final HqlTupleElementImpl tupleElement = new HqlTupleElementImpl(
						i,
						aliases == null ? null : aliases[i],
						resultTypes[i]
				);
				tupleElements.add( tupleElement );
				if ( hasAliases ) {
					final String alias = aliases[i];
					if ( alias != null ) {
						tupleElementsByAlias.put( alias, tupleElement );
					}
				}
			}
		}

		@Override
		public Object transformTuple(Object[] tuple, String[] aliases) {
			if ( tuple.length != tupleElements.size() ) {
				throw new IllegalArgumentException(
						"Size mismatch between tuple result [" + tuple.length + "] and expected tuple elements [" +
								tupleElements.size() + "]"
				);
			}
			return new HqlTupleImpl( tuple );
		}

		public static class HqlTupleElementImpl<X> implements TupleElement<X> {
			private final int position;
			private final String alias;
			private final Type hibernateType;

			public HqlTupleElementImpl(int position, String alias, Type hibernateType) {
				this.position = position;
				this.alias = alias;
				this.hibernateType = hibernateType;
			}

			@Override
			public Class getJavaType() {
				return hibernateType.getReturnedClass();
			}

			@Override
			public String getAlias() {
				return alias;
			}

			public int getPosition() {
				return position;
			}

			public Type getHibernateType() {
				return hibernateType;
			}
		}

		public class HqlTupleImpl implements Tuple {
			private Object[] tuple;

			public HqlTupleImpl(Object[] tuple) {
				this.tuple = tuple;
			}

			@Override
			public <X> X get(String alias, Class<X> type) {
				final Object untyped = get( alias );
				if ( untyped != null ) {
					if ( ! type.isInstance( untyped ) ) {
						throw new IllegalArgumentException(
								String.format(
										"Requested tuple value [alias=%s, value=%s] cannot be assigned to requested type [%s]",
										alias,
										untyped,
										type.getName()
								)
						);
					}
				}
				return (X) untyped;
			}

			@Override
			public Object get(String alias) {
				HqlTupleElementImpl tupleElement = tupleElementsByAlias.get( alias );
				if ( tupleElement == null ) {
					throw new IllegalArgumentException( "Unknown alias [" + alias + "]" );
				}
				return tuple[ tupleElement.getPosition() ];
			}

			@Override
			public <X> X get(int i, Class<X> type) {
				final Object result = get( i );
				if ( result != null && ! type.isInstance( result ) ) {
					throw new IllegalArgumentException(
							String.format(
									"Requested tuple value [index=%s, realType=%s] cannot be assigned to requested type [%s]",
									i,
									result.getClass().getName(),
									type.getName()
							)
					);
				}
				return ( X ) result;
			}

			@Override
			public Object get(int i) {
				if ( i < 0 ) {
					throw new IllegalArgumentException( "requested tuple index must be greater than zero" );
				}
				if ( i > tuple.length ) {
					throw new IllegalArgumentException( "requested tuple index exceeds actual tuple size" );
				}
				return tuple[i];
			}

			@Override
			public Object[] toArray() {
				// todo : make a copy?
				return tuple;
			}

			@Override
			public List<TupleElement<?>> getElements() {
				return tupleElements;
			}

			@Override
			public <X> X get(TupleElement<X> tupleElement) {
				if ( HqlTupleElementImpl.class.isInstance( tupleElement ) ) {
					return get( ( (HqlTupleElementImpl) tupleElement ).getPosition(), tupleElement.getJavaType() );
				}
				else {
					return get( tupleElement.getAlias(), tupleElement.getJavaType() );
				}
			}
		}
	}

	@Override
	public <T> QueryImpl<T> createQuery(
			String jpaqlString,
			Class<T> resultClass,
			Selection selection,
			QueryOptions queryOptions) {
		try {
			org.hibernate.Query hqlQuery = internalGetSession().createQuery( jpaqlString );

			if ( queryOptions.getValueHandlers() == null ) {
				if ( queryOptions.getResultMetadataValidator() != null ) {
					queryOptions.getResultMetadataValidator().validate( hqlQuery.getReturnTypes() );
				}
			}

			// determine if we need a result transformer
			List tupleElements = Tuple.class.equals( resultClass )
					? ( ( CompoundSelectionImpl<Tuple> ) selection ).getCompoundSelectionItems()
					: null;
			if ( queryOptions.getValueHandlers() != null || tupleElements != null ) {
				hqlQuery.setResultTransformer(
						new CriteriaQueryTransformer( queryOptions.getValueHandlers(), tupleElements )
				);
			}
			return new QueryImpl<T>( hqlQuery, this, queryOptions.getNamedParameterExplicitTypes() );
		}
		catch ( RuntimeException e ) {
			throw convert( e );
		}
	}

	private static class CriteriaQueryTransformer extends BasicTransformerAdapter {
		private final List<ValueHandlerFactory.ValueHandler> valueHandlers;
		private final List tupleElements;

		private CriteriaQueryTransformer(List<ValueHandlerFactory.ValueHandler> valueHandlers, List tupleElements) {
			// todo : should these 2 sizes match *always*?
			this.valueHandlers = valueHandlers;
			this.tupleElements = tupleElements;
		}

		@Override
		public Object transformTuple(Object[] tuple, String[] aliases) {
			final Object[] valueHandlerResult;
			if ( valueHandlers == null ) {
				valueHandlerResult = tuple;
			}
			else {
				valueHandlerResult = new Object[tuple.length];
				for ( int i = 0; i < tuple.length; i++ ) {
					ValueHandlerFactory.ValueHandler valueHandler = valueHandlers.get( i );
					valueHandlerResult[i] = valueHandler == null
							? tuple[i]
							: valueHandler.convert( tuple[i] );
				}
			}

			return tupleElements == null
					? valueHandlerResult.length == 1 ? valueHandlerResult[0] : valueHandlerResult
					: new TupleImpl( tuple );

		}

		private class TupleImpl implements Tuple {
			private final Object[] tuples;

			private TupleImpl(Object[] tuples) {
				if ( tuples.length != tupleElements.size() ) {
					throw new IllegalArgumentException(
							"Size mismatch between tuple result [" + tuples.length
									+ "] and expected tuple elements [" + tupleElements.size() + "]"
					);
				}
				this.tuples = tuples;
			}

			public <X> X get(TupleElement<X> tupleElement) {
				int index = tupleElements.indexOf( tupleElement );
				if ( index < 0 ) {
					throw new IllegalArgumentException(
							"Requested tuple element did not correspond to element in the result tuple"
					);
				}
				// index should be "in range" by nature of size check in ctor
				return ( X ) tuples[index];
			}

			public Object get(String alias) {
				int index = -1;
				if ( alias != null ) {
					alias = alias.trim();
					if ( alias.length() > 0 ) {
						int i = 0;
						for ( TupleElement selection : ( List<TupleElement> ) tupleElements ) {
							if ( alias.equals( selection.getAlias() ) ) {
								index = i;
								break;
							}
							i++;
						}
					}
				}
				if ( index < 0 ) {
					throw new IllegalArgumentException(
							"Given alias [" + alias + "] did not correspond to an element in the result tuple"
					);
				}
				// index should be "in range" by nature of size check in ctor
				return tuples[index];
			}

			public <X> X get(String alias, Class<X> type) {
				final Object untyped = get( alias );
				if ( untyped != null ) {
					if ( ! type.isInstance( untyped ) ) {
						throw new IllegalArgumentException(
								String.format(
										"Requested tuple value [alias=%s, value=%s] cannot be assigned to requested type [%s]",
										alias,
										untyped,
										type.getName()
								)
						);
					}
				}
				return (X) untyped;
			}

			public Object get(int i) {
				if ( i >= tuples.length ) {
					throw new IllegalArgumentException(
							"Given index [" + i + "] was outside the range of result tuple size [" + tuples.length + "] "
					);
				}
				return tuples[i];
			}

			public <X> X get(int i, Class<X> type) {
				final Object result = get( i );
				if ( result != null && ! type.isInstance( result ) ) {
					throw new IllegalArgumentException(
							String.format(
									"Requested tuple value [index=%s, realType=%s] cannot be assigned to requested type [%s]",
									i,
									result.getClass().getName(),
									type.getName()
							)
					);
				}
				return ( X ) result;
			}

			public Object[] toArray() {
				return tuples;
			}

			public List<TupleElement<?>> getElements() {
				return tupleElements;
			}
		}
	}

	private CriteriaCompiler criteriaCompiler;

	protected CriteriaCompiler criteriaCompiler() {
		if ( criteriaCompiler == null ) {
			criteriaCompiler = new CriteriaCompiler( this );
		}
		return criteriaCompiler;
	}

	@Override
	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		checkOpen();
		try {
			return (TypedQuery<T>) criteriaCompiler().compile( (CompilableCriteria) criteriaQuery );
		}
		catch ( RuntimeException e ) {
			throw convert( e );
		}
	}

	@Override
	public Query createQuery(CriteriaUpdate criteriaUpdate) {
		checkOpen();
		try {
			return criteriaCompiler().compile( (CompilableCriteria) criteriaUpdate );
		}
		catch ( RuntimeException e ) {
			throw convert( e );
		}
	}

	@Override
	public Query createQuery(CriteriaDelete criteriaDelete) {
		checkOpen();
		try {
			return criteriaCompiler().compile( (CompilableCriteria) criteriaDelete );
		}
		catch ( RuntimeException e ) {
			throw convert( e );
		}
	}

	@Override
	public Query createNamedQuery(String name) {
		return buildQueryFromName( name, null );
	}

	private QueryImpl buildQueryFromName(String name, Class resultType) {
		checkOpen();

		// we can't just call Session#getNamedQuery because we need to apply stored setting at the JPA Query
		// level too

		final SessionFactoryImplementor sfi = entityManagerFactory.getSessionFactory();

		final NamedQueryDefinition jpqlDefinition = sfi.getNamedQueryRepository().getNamedQueryDefinition( name );
		if ( jpqlDefinition != null ) {
			return createNamedJpqlQuery( jpqlDefinition, resultType );
		}

		final NamedSQLQueryDefinition nativeQueryDefinition = sfi.getNamedQueryRepository().getNamedSQLQueryDefinition( name );
		if ( nativeQueryDefinition != null ) {
			return createNamedSqlQuery( nativeQueryDefinition, resultType );
		}

		throw convert( new IllegalArgumentException( "No query defined for that name [" + name + "]" ) );
	}

	protected QueryImpl createNamedJpqlQuery(NamedQueryDefinition namedQueryDefinition, Class resultType) {
		final org.hibernate.Query hibQuery = ( (SessionImplementor) internalGetSession() ).createQuery( namedQueryDefinition );
		if ( resultType != null ) {
			resultClassChecking( resultType, hibQuery );
		}

		return wrapAsJpaQuery( namedQueryDefinition, hibQuery );
	}

	protected QueryImpl wrapAsJpaQuery(NamedQueryDefinition namedQueryDefinition, org.hibernate.Query hibQuery) {
		try {
			final QueryImpl jpaQuery = new QueryImpl( hibQuery, this );
			applySavedSettings( namedQueryDefinition, jpaQuery );
			return jpaQuery;
		}
		catch ( RuntimeException e ) {
			throw convert( e );
		}
	}

	protected void applySavedSettings(NamedQueryDefinition namedQueryDefinition, QueryImpl jpaQuery) {
		if ( namedQueryDefinition.isCacheable() ) {
			jpaQuery.setHint( QueryHints.HINT_CACHEABLE, true );
			if ( namedQueryDefinition.getCacheRegion() != null ) {
				jpaQuery.setHint( QueryHints.HINT_CACHE_REGION, namedQueryDefinition.getCacheRegion() );
			}
		}

		if ( namedQueryDefinition.getCacheMode() != null ) {
			jpaQuery.setHint( QueryHints.HINT_CACHE_MODE, namedQueryDefinition.getCacheMode() );
		}

		if ( namedQueryDefinition.isReadOnly() ) {
			jpaQuery.setHint( QueryHints.HINT_READONLY, true );
		}

		if ( namedQueryDefinition.getTimeout() != null ) {
			jpaQuery.setHint( QueryHints.SPEC_HINT_TIMEOUT, namedQueryDefinition.getTimeout() * 1000 );
		}

		if ( namedQueryDefinition.getFetchSize() != null ) {
			jpaQuery.setHint( QueryHints.HINT_FETCH_SIZE, namedQueryDefinition.getFetchSize() );
		}

		if ( namedQueryDefinition.getComment() != null ) {
			jpaQuery.setHint( QueryHints.HINT_COMMENT, namedQueryDefinition.getComment() );
		}

		if ( namedQueryDefinition.getFirstResult() != null ) {
			jpaQuery.setFirstResult( namedQueryDefinition.getFirstResult() );
		}

		if ( namedQueryDefinition.getMaxResults() != null ) {
			jpaQuery.setMaxResults( namedQueryDefinition.getMaxResults() );
		}

		if ( namedQueryDefinition.getLockOptions() != null ) {
			if ( namedQueryDefinition.getLockOptions().getLockMode() != null ) {
				jpaQuery.setLockMode(
						LockModeTypeHelper.getLockModeType( namedQueryDefinition.getLockOptions().getLockMode() )
				);
			}
		}

		if ( namedQueryDefinition.getFlushMode() != null ) {
			if ( namedQueryDefinition.getFlushMode() == FlushMode.COMMIT ) {
				jpaQuery.setFlushMode( FlushModeType.COMMIT );
			}
			else {
				jpaQuery.setFlushMode( FlushModeType.AUTO );
			}
		}
	}

	protected QueryImpl createNamedSqlQuery(NamedSQLQueryDefinition namedQueryDefinition, Class resultType) {
		if ( resultType != null ) {
			resultClassChecking( resultType, namedQueryDefinition );
		}
		return wrapAsJpaQuery(
				namedQueryDefinition,
				( (SessionImplementor) internalGetSession() ).createSQLQuery( namedQueryDefinition )
		);
	}

	protected void resultClassChecking(Class resultType, NamedSQLQueryDefinition namedQueryDefinition) {
		final SessionFactoryImplementor sfi = entityManagerFactory.getSessionFactory();

		final NativeSQLQueryReturn[] queryReturns;
		if ( namedQueryDefinition.getQueryReturns() != null ) {
			queryReturns = namedQueryDefinition.getQueryReturns();
		}
		else if ( namedQueryDefinition.getResultSetRef() != null ) {
			final ResultSetMappingDefinition rsMapping = sfi.getResultSetMapping( namedQueryDefinition.getResultSetRef() );
			queryReturns = rsMapping.getQueryReturns();
		}
		else {
			throw new AssertionFailure( "Unsupported named query model. Please report the bug in Hibernate EntityManager");
		}

		if ( queryReturns.length > 1 ) {
			throw new IllegalArgumentException( "Cannot create TypedQuery for query with more than one return" );
		}

		final NativeSQLQueryReturn nativeSQLQueryReturn = queryReturns[0];

		if ( nativeSQLQueryReturn instanceof NativeSQLQueryRootReturn ) {
			final Class<?> actualReturnedClass;
			final String entityClassName = ( (NativeSQLQueryRootReturn) nativeSQLQueryReturn ).getReturnEntityName();
			try {
				actualReturnedClass = sfi.getServiceRegistry().getService( ClassLoaderService.class ).classForName( entityClassName );
			}
			catch ( ClassLoadingException e ) {
				throw new AssertionFailure(
						"Unable to load class [" + entityClassName + "] declared on named native query [" +
								namedQueryDefinition.getName() + "]"
				);
			}
			if ( !resultType.isAssignableFrom( actualReturnedClass ) ) {
				throw buildIncompatibleException( resultType, actualReturnedClass );
			}
		}
		else if ( nativeSQLQueryReturn instanceof NativeSQLQueryConstructorReturn ) {
			final NativeSQLQueryConstructorReturn ctorRtn = (NativeSQLQueryConstructorReturn) nativeSQLQueryReturn;
			if ( !resultType.isAssignableFrom( ctorRtn.getTargetClass() ) ) {
				throw buildIncompatibleException( resultType, ctorRtn.getTargetClass() );
			}
		}
		else {
			//TODO support other NativeSQLQueryReturn type. For now let it go.
		}
	}

	@Override
	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
		return buildQueryFromName( name, resultClass );
	}

	private IllegalArgumentException buildIncompatibleException(Class<?> resultClass, Class<?> actualResultClass) {
		return new IllegalArgumentException(
				"Type specified for TypedQuery [" + resultClass.getName() +
						"] is incompatible with query return type [" + actualResultClass + "]"
		);
	}

	@Override
	public Query createNativeQuery(String sqlString) {
		checkOpen();
		try {
			SQLQuery q = internalGetSession().createSQLQuery( sqlString );
			return new QueryImpl( q, this );
		}
		catch ( RuntimeException he ) {
			throw convert( he );
		}
	}

	@Override
	public Query createNativeQuery(String sqlString, Class resultClass) {
		checkOpen();
		try {
			SQLQuery q = internalGetSession().createSQLQuery( sqlString );
			q.addEntity( "alias1", resultClass.getName(), LockMode.READ );
			return new QueryImpl( q, this );
		}
		catch ( RuntimeException he ) {
			throw convert( he );
		}
	}

	@Override
	public Query createNativeQuery(String sqlString, String resultSetMapping) {
		checkOpen();
		try {
			final SQLQuery q = internalGetSession().createSQLQuery( sqlString );
			q.setResultSetMapping( resultSetMapping );
			return new QueryImpl( q, this );
		}
		catch ( RuntimeException he ) {
			throw convert( he );
		}
	}

	@Override
	public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
		checkOpen();
		try {
			final ProcedureCallMemento memento = ( (SessionImplementor) internalGetSession() ).getFactory()
					.getNamedQueryRepository().getNamedProcedureCallMemento( name );
			if ( memento == null ) {
				throw new IllegalArgumentException( "No @NamedStoredProcedureQuery was found with that name : " + name );
			}
			final StoredProcedureQueryImpl jpaImpl = new StoredProcedureQueryImpl( memento, this );
			// apply hints
			if ( memento.getHintsMap() != null ) {
				for ( Map.Entry<String,Object> hintEntry : memento.getHintsMap().entrySet() ) {
					jpaImpl.setHint( hintEntry.getKey(), hintEntry.getValue() );
				}
			}
			return jpaImpl;
		}
		catch ( RuntimeException e ) {
			throw convert( e );
		}
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
		checkOpen();
		try {
			return new StoredProcedureQueryImpl(
					internalGetSession().createStoredProcedureCall( procedureName ),
					this
			);
		}
		catch ( RuntimeException e ) {
			throw convert( e );
		}
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
		checkOpen();
		try {
			return new StoredProcedureQueryImpl(
					internalGetSession().createStoredProcedureCall( procedureName, resultClasses ),
					this
			);
		}
		catch ( RuntimeException e ) {
			throw convert( e );
		}
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		checkOpen();
		try {
			try {
				return new StoredProcedureQueryImpl(
						internalGetSession().createStoredProcedureCall( procedureName, resultSetMappings ),
						this
				);
			}
			catch (UnknownSqlResultSetMappingException unknownResultSetMapping) {
				throw new IllegalArgumentException( unknownResultSetMapping.getMessage(), unknownResultSetMapping );
			}
		}
		catch ( RuntimeException e ) {
			throw convert( e );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getReference(Class<T> entityClass, Object primaryKey) {
		checkOpen();
		try {
			return ( T ) internalGetSession().load( entityClass, ( Serializable ) primaryKey );
		}
		catch ( MappingException e ) {
			throw convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( TypeMismatchException e ) {
			throw convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( ClassCastException e ) {
			throw convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			throw convert( e );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A> A find(Class<A> entityClass, Object primaryKey) {
		checkOpen();
		return find( entityClass, primaryKey, null, null );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
		checkOpen();
		return find( entityClass, primaryKey, null, properties );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A> A find(Class<A> entityClass, Object primaryKey, LockModeType lockModeType) {
		checkOpen();
		return find( entityClass, primaryKey, lockModeType, null );
	}

	@Override
	public <A> A find(Class<A> entityClass, Object primaryKey, LockModeType lockModeType, Map<String, Object> properties) {
		checkOpen();
		Session session = internalGetSession();
		CacheMode previousCacheMode = session.getCacheMode();
		CacheMode cacheMode = determineAppropriateLocalCacheMode( properties );
		LockOptions lockOptions = null;
		try {
			if ( properties != null && !properties.isEmpty() ) {
				( (SessionImplementor) session ).getLoadQueryInfluencers()
						.setFetchGraph( (EntityGraph) properties.get( QueryHints.HINT_FETCHGRAPH ) );
				( (SessionImplementor) session ).getLoadQueryInfluencers()
						.setLoadGraph( (EntityGraph) properties.get( QueryHints.HINT_LOADGRAPH ) );
			}
			session.setCacheMode( cacheMode );
			if ( lockModeType != null ) {
				lockOptions = getLockRequest( lockModeType, properties );
				if ( !LockModeType.NONE.equals( lockModeType) ) {
					checkTransactionNeeded();
				}
				return ( A ) session.get(
						entityClass, ( Serializable ) primaryKey, 
						lockOptions
				);
			}
			else {
				return ( A ) session.get( entityClass, ( Serializable ) primaryKey );
			}
		}
		catch ( EntityNotFoundException ignored ) {
			// DefaultLoadEventListener.returnNarrowedProxy may throw ENFE (see HHH-7861 for details),
			// which find() should not throw.  Find() should return null if the entity was not found.
			if ( LOG.isDebugEnabled() ) {
				String entityName = entityClass != null ? entityClass.getName(): null;
				String identifierValue = primaryKey != null ? primaryKey.toString() : null ;
				LOG.ignoringEntityNotFound( entityName, identifierValue );
			}
			return null;
		}
		catch ( ObjectDeletedException e ) {
			//the spec is silent about people doing remove() find() on the same PC
			return null;
		}
		catch ( ObjectNotFoundException e ) {
			//should not happen on the entity itself with get
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( MappingException e ) {
			throw convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( TypeMismatchException e ) {
			throw convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( ClassCastException e ) {
			throw convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			throw convert( e, lockOptions );
		}
		finally {
			session.setCacheMode( previousCacheMode );
			( (SessionImplementor) session ).getLoadQueryInfluencers().setFetchGraph( null );
			( (SessionImplementor) session ).getLoadQueryInfluencers().setLoadGraph( null );

		}
	}

	public CacheMode determineAppropriateLocalCacheMode(Map<String, Object> localProperties) {
		CacheRetrieveMode retrieveMode = null;
		CacheStoreMode storeMode = null;
		if ( localProperties != null ) {
			retrieveMode = determineCacheRetrieveMode( localProperties );
			storeMode = determineCacheStoreMode( localProperties );
		}
		if ( retrieveMode == null ) {
			// use the EM setting
			retrieveMode = determineCacheRetrieveMode( this.properties );
		}
		if ( storeMode == null ) {
			// use the EM setting
			storeMode = determineCacheStoreMode( this.properties );
		}
		return CacheModeHelper.interpretCacheMode( storeMode, retrieveMode );
	}

	private void checkTransactionNeeded() {
		if ( !isTransactionInProgress() ) {
			throw new TransactionRequiredException(
					"no transaction is in progress"
			);
		}
	}

	@Override
	public void persist(Object entity) {
		checkOpen();
		try {
			internalGetSession().persist( entity );
		}
		catch ( MappingException e ) {
			throw convert( new IllegalArgumentException( e.getMessage() ) ) ;
		}
		catch ( RuntimeException e ) {
			throw convert( e );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A> A merge(A entity) {
		checkOpen();
		try {
			return ( A ) internalGetSession().merge( entity );
		}
		catch ( ObjectDeletedException sse ) {
			throw convert( new IllegalArgumentException( sse ) );
		}
		catch ( MappingException e ) {
			throw convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			//including HibernateException
			throw convert( e );
		}
	}

	@Override
	public void remove(Object entity) {
		checkOpen();
		try {
			internalGetSession().delete( entity );
		}
		catch ( MappingException e ) {
			throw convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch ( RuntimeException e ) {
			//including HibernateException
			throw convert( e );
		}
	}

	@Override
	public void refresh(Object entity) {
		refresh( entity, null, null );
	}

	@Override
	public void refresh(Object entity, Map<String, Object> properties) {
		refresh( entity, null, properties );
	}

	@Override
	public void refresh(Object entity, LockModeType lockModeType) {
		refresh( entity, lockModeType, null );
	}

	@Override
	public void refresh(Object entity, LockModeType lockModeType, Map<String, Object> properties) {
		checkOpen();

		final Session session = internalGetSession();
		final CacheMode previousCacheMode = session.getCacheMode();
		final CacheMode localCacheMode = determineAppropriateLocalCacheMode( properties );
		LockOptions lockOptions = null;
		try {
			session.setCacheMode( localCacheMode );
			if ( !session.contains( entity ) ) {
				throw convert ( new IllegalArgumentException( "Entity not managed" ) );
			}
			if ( lockModeType != null ) {
				if ( !LockModeType.NONE.equals( lockModeType) ) {
					checkTransactionNeeded();
				}

				lockOptions = getLockRequest( lockModeType, properties );
				session.refresh( entity, lockOptions );
			}
			else {
				session.refresh( entity );
			}
		}
		catch (MappingException e) {
			throw convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch (RuntimeException e) {
			throw convert( e, lockOptions );
		}
		finally {
			session.setCacheMode( previousCacheMode );
		}
	}

	@Override
	public boolean contains(Object entity) {
		checkOpen();

		try {
			if ( entity != null
					&& !( entity instanceof HibernateProxy )
					&& internalGetSession().getSessionFactory().getClassMetadata( entity.getClass() ) == null ) {
				throw convert( new IllegalArgumentException( "Not an entity:" + entity.getClass() ) );
			}
			return internalGetSession().contains( entity );
		}
		catch (MappingException e) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch (RuntimeException e) {
			throw convert( e );
		}
	}

	@Override
	public LockModeType getLockMode(Object entity) {
		checkOpen();

		if ( !isTransactionInProgress() ) {
			throw new TransactionRequiredException( "Call to EntityManager#getLockMode should occur within transaction according to spec" );
		}

		if ( !contains( entity ) ) {
			throw convert( new IllegalArgumentException( "entity not in the persistence context" ) );
		}

		return getLockModeType( internalGetSession().getCurrentLockMode( entity ) );
	}

	@Override
	public void setProperty(String s, Object o) {
		checkOpen();

		if ( ENTITY_MANAGER_SPECIFIC_PROPERTIES.contains( s ) ) {
			properties.put( s, o );
			applyProperties();
        }
		else {
			LOG.debugf("Trying to set a property which is not supported on entity manager level");
		}
	}

	@Override
	public Map<String, Object> getProperties() {
		return Collections.unmodifiableMap( properties );
	}

	@Override
	public void flush() {
		checkOpen();
		checkTransactionNeeded();

		try {
			internalGetSession().flush();
		}
		catch ( RuntimeException e ) {
			throw convert( e );
		}
	}

	/**
	 * return a Session
	 *
	 * @throws IllegalStateException if the entity manager is closed
	 */
	public abstract Session getSession();

	/**
	 * Return a Session (even if the entity manager is closed).
	 *
	 * @return A session.
	 * @deprecated Deprecated in favor of {@link #getRawSession()}
	 */
	@Deprecated
	protected abstract Session getRawSession();

	/**
	 * Return a Session without any validation checks.
	 *
	 * @return A session.
	 */
	protected abstract Session internalGetSession();

	@Override
	public EntityTransaction getTransaction() {
		if ( transactionType == PersistenceUnitTransactionType.JTA ) {
			throw new IllegalStateException( "A JTA EntityManager cannot use getTransaction()" );
		}
		return tx;
	}

	@Override
	public EntityManagerFactoryImpl getEntityManagerFactory() {
		checkOpen();
		return internalGetEntityManagerFactory();
	}

	protected EntityManagerFactoryImpl internalGetEntityManagerFactory() {
		return entityManagerFactory;
	}

	@Override
	public HibernateEntityManagerFactory getFactory() {
		return entityManagerFactory;
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder() {

		checkOpen();
		return getEntityManagerFactory().getCriteriaBuilder();
	}

	@Override
	public Metamodel getMetamodel() {
		checkOpen();
		return getEntityManagerFactory().getMetamodel();
	}

	@Override
	public void setFlushMode(FlushModeType flushModeType) {
		checkOpen();
		if ( flushModeType == FlushModeType.AUTO ) {
			internalGetSession().setFlushMode( FlushMode.AUTO );
		}
		else if ( flushModeType == FlushModeType.COMMIT ) {
			internalGetSession().setFlushMode( FlushMode.COMMIT );
		}
		else {
			throw new AssertionFailure( "Unknown FlushModeType: " + flushModeType );
		}
	}

	@Override
	public void clear() {
		checkOpen();
		try {
			internalGetSession().clear();
		}
		catch (RuntimeException e) {
			throw convert( e );
		}
	}

	@Override
	public void detach(Object entity) {
		checkOpen();
		try {
			internalGetSession().evict( entity );
		}
		catch (RuntimeException e) {
			throw convert( e );
		}
	}

	/**
	 * Hibernate can be set in various flush modes that are unknown to
	 * JPA 2.0. This method can then return null.
	 * If it returns null, do em.unwrap(Session.class).getFlushMode() to get the
	 * Hibernate flush mode
	 */
	@Override
	public FlushModeType getFlushMode() {
		checkOpen();

		FlushMode mode = internalGetSession().getFlushMode();
		if ( mode == FlushMode.AUTO ) {
			return FlushModeType.AUTO;
		}
		else if ( mode == FlushMode.COMMIT ) {
			return FlushModeType.COMMIT;
		}
		else {
			// otherwise this is an unknown mode for EJB3
			return null;
		}
	}

	public void lock(Object entity, LockModeType lockMode) {
		lock( entity, lockMode, null );
	}

	public void lock(Object entity, LockModeType lockModeType, Map<String, Object> properties) {
		checkOpen();
		checkTransactionNeeded();

		LockOptions lockOptions = null;

		try {
			if ( !contains( entity ) ) {
				throw new IllegalArgumentException( "entity not in the persistence context" );
			}
			lockOptions = getLockRequest( lockModeType, properties );
			internalGetSession().buildLockRequest( lockOptions ).lock( entity );
		}
		catch (RuntimeException e) {
			throw convert( e, lockOptions );
		}
	}

	public LockOptions getLockRequest(LockModeType lockModeType, Map<String, Object> properties) {
		LockOptions lockOptions = new LockOptions();
		LockOptions.copy( this.lockOptions, lockOptions );
		lockOptions.setLockMode( getLockMode( lockModeType ) );
		if ( properties != null ) {
			setLockOptions( properties, lockOptions );
		}
		return lockOptions;
	}

	@SuppressWarnings("deprecation")
	private static LockModeType getLockModeType(LockMode lockMode) {
		//TODO check that if we have UPGRADE_NOWAIT we have a timeout of zero?
		return LockModeTypeHelper.getLockModeType( lockMode );
	}


	private static LockMode getLockMode(LockModeType lockMode) {
		return LockModeTypeHelper.getLockMode( lockMode );
	}

	public boolean isTransactionInProgress() {
		return ( ( SessionImplementor ) internalGetSession() ).isTransactionInProgress();
	}

	private SessionFactoryImplementor sfi() {
		return (SessionFactoryImplementor) internalGetSession().getSessionFactory();
	}

	@Override
	public <T> T unwrap(Class<T> clazz) {
		checkOpen();

		if ( Session.class.isAssignableFrom( clazz ) ) {
			return ( T ) internalGetSession();
		}
		if ( SessionImplementor.class.isAssignableFrom( clazz ) ) {
			return ( T ) internalGetSession();
		}
		if ( EntityManager.class.isAssignableFrom( clazz ) ) {
			return ( T ) this;
		}
		throw new PersistenceException( "Hibernate cannot unwrap " + clazz );
	}

	@Override
	public void markForRollbackOnly() {
        LOG.debugf("Mark transaction for rollback");
		if ( tx.isActive() ) {
			tx.setRollbackOnly();
		}
		else {
			//no explicit use of the tx. boundaries methods
			if ( PersistenceUnitTransactionType.JTA == transactionType ) {
				TransactionManager transactionManager = sfi().getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager();
				if ( transactionManager == null ) {
					throw new PersistenceException(
							"Using a JTA persistence context wo setting hibernate.transaction.jta.platform"
					);
				}
				try {
					if ( transactionManager.getStatus() != Status.STATUS_NO_TRANSACTION ) {
						transactionManager.setRollbackOnly();
					}
				}
				catch (SystemException e) {
					throw new PersistenceException( "Unable to set the JTA transaction as RollbackOnly", e );
				}
			}
		}
	}

	@Override
	public boolean isJoinedToTransaction() {
		checkOpen();

		final SessionImplementor session = (SessionImplementor) internalGetSession();
		final TransactionCoordinator transactionCoordinator = session.getTransactionCoordinator();
		final TransactionImplementor transaction = transactionCoordinator.getTransaction();

		return isOpen() && transaction.getJoinStatus() == JoinStatus.JOINED;
	}

	@Override
	public void joinTransaction() {
		checkOpen();
		joinTransaction( true );
	}

	private void joinTransaction(boolean explicitRequest) {
		if ( transactionType != PersistenceUnitTransactionType.JTA ) {
			if ( explicitRequest ) {
			    LOG.callingJoinTransactionOnNonJtaEntityManager();
			}
			return;
		}

		final SessionImplementor session = (SessionImplementor) internalGetSession();
		final TransactionCoordinator transactionCoordinator = session.getTransactionCoordinator();
		final TransactionImplementor transaction = transactionCoordinator.getTransaction();

		transaction.markForJoin();
		transactionCoordinator.pulse();

		LOG.debug( "Looking for a JTA transaction to join" );
		if ( ! transactionCoordinator.isTransactionJoinable() ) {
			if ( explicitRequest ) {
				// if this is an explicit join request, log a warning so user can track underlying cause
				// of subsequent exceptions/messages
				LOG.unableToJoinTransaction(Environment.TRANSACTION_STRATEGY);
			}
		}

		try {
			if ( transaction.getJoinStatus() == JoinStatus.JOINED ) {
				LOG.debug( "Transaction already joined" );
				return; // noop
			}

			// join the transaction and then recheck the status
			transaction.join();
			if ( transaction.getJoinStatus() == JoinStatus.NOT_JOINED ) {
				if ( explicitRequest ) {
					throw new TransactionRequiredException( "No active JTA transaction on joinTransaction call" );
				}
				else {
					LOG.debug( "Unable to join JTA transaction" );
					return;
				}
			}
			else if ( transaction.getJoinStatus() == JoinStatus.MARKED_FOR_JOINED ) {
				throw new AssertionFailure( "Transaction MARKED_FOR_JOINED after isOpen() call" );
			}

			// register behavior changes
			final SynchronizationCallbackCoordinator callbackCoordinator = transactionCoordinator.getSynchronizationCallbackCoordinator();
			callbackCoordinator.setManagedFlushChecker( new ManagedFlushCheckerImpl() );
			callbackCoordinator.setExceptionMapper( new CallbackExceptionMapperImpl() );
			callbackCoordinator.setAfterCompletionAction( new AfterCompletionActionImpl( session, transactionType ) );
		}
		catch (HibernateException he) {
			throw convert( he );
		}
	}

	/**
	 * returns the underlying session
	 */
	public Object getDelegate() {
		checkOpen();
		return internalGetSession();
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		tx = new TransactionImpl( this );
	}

	@Override
	public void handlePersistenceException(PersistenceException e) {
		if ( e instanceof NoResultException ) {
			return;
		}
		if ( e instanceof NonUniqueResultException ) {
			return;
		}
		if ( e instanceof LockTimeoutException ) {
			return;
		}
		if ( e instanceof QueryTimeoutException ) {
			return;
		}

		try {
			markForRollbackOnly();
		}
		catch ( Exception ne ) {
			//we do not want the subsequent exception to swallow the original one
            LOG.unableToMarkForRollbackOnPersistenceException(ne);
		}
	}

	@Override
	public void throwPersistenceException(PersistenceException e) {
		handlePersistenceException( e );
		throw e;
	}

	@Override
	public RuntimeException convert(HibernateException e) {
		//FIXME should we remove all calls to this method and use convert(RuntimeException) ?
		return convert( e, null );
	}

	public RuntimeException convert(RuntimeException e) {
		RuntimeException result = e;
		if ( e instanceof HibernateException ) {
			result = convert( (HibernateException) e );
		}
		else {
			markForRollbackOnly();
		}
		return result;
	}

	public RuntimeException convert(RuntimeException e, LockOptions lockOptions) {
		RuntimeException result = e;
		if ( e instanceof HibernateException ) {
			result = convert( (HibernateException) e , lockOptions );
		}
		else {
			markForRollbackOnly();
		}
		return result;
	}

	@Override
	public RuntimeException convert(HibernateException e, LockOptions lockOptions) {
		if ( e instanceof StaleStateException ) {
			final PersistenceException converted = wrapStaleStateException( (StaleStateException) e );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof LockingStrategyException ) {
			final PersistenceException converted = wrapLockException( e, lockOptions );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof org.hibernate.exception.LockTimeoutException ) {
			final PersistenceException converted = wrapLockException( e, lockOptions );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof org.hibernate.PessimisticLockException ) {
			final PersistenceException converted = wrapLockException( e, lockOptions );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof org.hibernate.QueryTimeoutException ) {
			final QueryTimeoutException converted = new QueryTimeoutException( e.getMessage(), e );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof ObjectNotFoundException ) {
			final EntityNotFoundException converted = new EntityNotFoundException( e.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof org.hibernate.NonUniqueObjectException ) {
			final EntityExistsException converted = new EntityExistsException( e.getMessage() );
			handlePersistenceException( converted );
			return converted;
        }
		else if ( e instanceof org.hibernate.NonUniqueResultException ) {
			final NonUniqueResultException converted = new NonUniqueResultException( e.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof UnresolvableObjectException ) {
			final EntityNotFoundException converted = new EntityNotFoundException( e.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof QueryException ) {
			return new IllegalArgumentException( e );
		}
		else if ( e instanceof TransientObjectException ) {
			try {
				markForRollbackOnly();
			}
			catch ( Exception ne ) {
				//we do not want the subsequent exception to swallow the original one
				LOG.unableToMarkForRollbackOnTransientObjectException( ne );
			}
			return new IllegalStateException( e ); //Spec 3.2.3 Synchronization rules
		}
		else {
			final PersistenceException converted = new PersistenceException( e );
			handlePersistenceException( converted );
			return converted;
		}
	}

	@Override
	public void throwPersistenceException(HibernateException e) {
		throw convert( e );
	}

	@Override
	public PersistenceException wrapStaleStateException(StaleStateException e) {
		PersistenceException pe;
		if ( e instanceof StaleObjectStateException ) {
			final StaleObjectStateException sose = (StaleObjectStateException) e;
			final Serializable identifier = sose.getIdentifier();
			if ( identifier != null ) {
				try {
					final Object entity = internalGetSession().load( sose.getEntityName(), identifier );
					if ( entity instanceof Serializable ) {
						//avoid some user errors regarding boundary crossing
						pe = new OptimisticLockException( e.getMessage(), e, entity );
					}
					else {
						pe = new OptimisticLockException( e.getMessage(), e );
					}
				}
				catch ( EntityNotFoundException enfe ) {
					pe = new OptimisticLockException( e.getMessage(), e );
				}
			}
			else {
				pe = new OptimisticLockException( e.getMessage(), e );
			}
		}
		else {
			pe = new OptimisticLockException( e.getMessage(), e );
		}
		return pe;
	}

	public PersistenceException wrapLockException(HibernateException e, LockOptions lockOptions) {
		final PersistenceException pe;
		if ( e instanceof OptimisticEntityLockException ) {
			final OptimisticEntityLockException lockException = (OptimisticEntityLockException) e;
			pe = new OptimisticLockException( lockException.getMessage(), lockException, lockException.getEntity() );
		}
		else if ( e instanceof org.hibernate.exception.LockTimeoutException ) {
			pe = new LockTimeoutException( e.getMessage(), e, null );
		}
		else if ( e instanceof PessimisticEntityLockException ) {
			final PessimisticEntityLockException lockException = (PessimisticEntityLockException) e;
			if ( lockOptions != null && lockOptions.getTimeOut() > -1 ) {
				// assume lock timeout occurred if a timeout or NO WAIT was specified
				pe = new LockTimeoutException( lockException.getMessage(), lockException, lockException.getEntity() );
			}
			else {
				pe = new PessimisticLockException( lockException.getMessage(), lockException, lockException.getEntity() );
			}
		}
		else if ( e instanceof org.hibernate.PessimisticLockException ) {
			final org.hibernate.PessimisticLockException jdbcLockException = (org.hibernate.PessimisticLockException) e;
			if ( lockOptions != null && lockOptions.getTimeOut() > -1 ) {
				// assume lock timeout occurred if a timeout or NO WAIT was specified
				pe = new LockTimeoutException( jdbcLockException.getMessage(), jdbcLockException, null );
			}
			else {
				pe = new PessimisticLockException( jdbcLockException.getMessage(), jdbcLockException, null );
			}
		}
		else {
			pe = new OptimisticLockException( e );
		}
		return pe;
	}

	private static class AfterCompletionActionImpl implements AfterCompletionAction {
		private final SessionImplementor session;
		private final PersistenceUnitTransactionType transactionType;

		private AfterCompletionActionImpl(SessionImplementor session, PersistenceUnitTransactionType transactionType) {
			this.session = session;
			this.transactionType = transactionType;
		}

		@Override
		public void doAction(TransactionCoordinator transactionCoordinator, int status) {
			if ( session.isClosed() ) {
                LOG.trace("Session was closed; nothing to do");
				return;
			}

			final boolean successful = JtaStatusHelper.isCommitted( status );
			if ( !successful && transactionType == PersistenceUnitTransactionType.JTA ) {
				( (Session) session ).clear();
			}
			session.getTransactionCoordinator().resetJoinStatus();
		}
	}

	private static class ManagedFlushCheckerImpl implements ManagedFlushChecker {
		@Override
		public boolean shouldDoManagedFlush(TransactionCoordinator coordinator, int jtaStatus) {
			return !coordinator.getTransactionContext().isClosed()
					&& !coordinator.getTransactionContext().isFlushModeNever()
					&& !JtaStatusHelper.isRollback( jtaStatus );
		}
	}

	private class CallbackExceptionMapperImpl implements ExceptionMapper {
		@Override
		public RuntimeException mapStatusCheckFailure(String message, SystemException systemException) {
			throw new PersistenceException( message, systemException );
		}

		@Override
		public RuntimeException mapManagedFlushFailure(String message, RuntimeException failure) {
			if ( HibernateException.class.isInstance( failure ) ) {
				throw convert( failure );
			}
			if ( PersistenceException.class.isInstance( failure ) ) {
				throw failure;
			}
			throw new PersistenceException( message, failure );
		}
	}
}
