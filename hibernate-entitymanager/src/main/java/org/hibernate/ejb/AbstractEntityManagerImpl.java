/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.ejb;

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
import javax.persistence.TransactionRequiredException;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.jboss.logging.Logger;

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
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.lock.LockingStrategyException;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.hibernate.ejb.criteria.CriteriaQueryCompiler;
import org.hibernate.ejb.criteria.ValueHandlerFactory;
import org.hibernate.ejb.criteria.expression.CompoundSelectionImpl;
import org.hibernate.ejb.internal.EntityManagerMessageLogger;
import org.hibernate.ejb.util.CacheModeHelper;
import org.hibernate.ejb.util.ConfigurationHelper;
import org.hibernate.ejb.util.LockModeTypeHelper;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.spi.JoinStatus;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.engine.transaction.synchronization.spi.AfterCompletionAction;
import org.hibernate.engine.transaction.synchronization.spi.ExceptionMapper;
import org.hibernate.engine.transaction.synchronization.spi.ManagedFlushChecker;
import org.hibernate.engine.transaction.synchronization.spi.SynchronizationCallbackCoordinator;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
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

    private static final EntityManagerMessageLogger LOG = Logger.getMessageLogger(EntityManagerMessageLogger.class,
                                                                           AbstractEntityManagerImpl.class.getName());

	private static final List<String> entityManagerSpecificProperties = new ArrayList<String>();

	static {
		entityManagerSpecificProperties.add( AvailableSettings.LOCK_SCOPE );
		entityManagerSpecificProperties.add( AvailableSettings.LOCK_TIMEOUT );
		entityManagerSpecificProperties.add( AvailableSettings.FLUSH_MODE );
		entityManagerSpecificProperties.add( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE );
		entityManagerSpecificProperties.add( AvailableSettings.SHARED_CACHE_STORE_MODE );
		entityManagerSpecificProperties.add( QueryHints.SPEC_HINT_TIMEOUT );
	}

	private EntityManagerFactoryImpl entityManagerFactory;
	protected transient TransactionImpl tx = new TransactionImpl( this );
	protected PersistenceContextType persistenceContextType;
	private PersistenceUnitTransactionType transactionType;
	private Map<String, Object> properties;
	private LockOptions lockOptions;

	protected AbstractEntityManagerImpl(
			EntityManagerFactoryImpl entityManagerFactory,
			PersistenceContextType type,
			PersistenceUnitTransactionType transactionType,
			Map properties) {
		this.entityManagerFactory = entityManagerFactory;
		this.persistenceContextType = type;
		this.transactionType = transactionType;

		this.lockOptions = new LockOptions();
		this.properties = new HashMap<String, Object>();
		for ( String key : entityManagerSpecificProperties ) {
			if ( entityManagerFactory.getProperties().containsKey( key ) ) {
				this.properties.put( key, entityManagerFactory.getProperties().get( key ) );
			}
			if ( properties != null && properties.containsKey( key ) ) {
				this.properties.put( key, properties.get( key ) );
			}
		}
	}

	public PersistenceUnitTransactionType getTransactionType() {
		return transactionType;
	}

	protected void postInit() {
		//register in Sync if needed
		if ( PersistenceUnitTransactionType.JTA.equals( transactionType ) ) {
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
			query.setHint ( QueryHints.SPEC_HINT_TIMEOUT, queryTimeout );
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
			if ( timeout < 0 ) {
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

	public Query createQuery(String jpaqlString) {
		try {
			return applyProperties( new QueryImpl<Object>( getSession().createQuery( jpaqlString ), this ) );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	public <T> TypedQuery<T> createQuery(String jpaqlString, Class<T> resultClass) {
		try {
			// do the translation
			org.hibernate.Query hqlQuery = getSession().createQuery( jpaqlString );

			resultClassChecking( resultClass, hqlQuery );

			// finally, build/return the query instance
			return new QueryImpl<T>( hqlQuery, this );
		}
		catch ( HibernateException he ) {
			throw convert( he );
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
				return (X) get( alias );
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
				return (X) get( i );
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

	public <T> TypedQuery<T> createQuery(
			String jpaqlString,
			Class<T> resultClass,
			Selection selection,
			Options options) {
		try {
			org.hibernate.Query hqlQuery = getSession().createQuery( jpaqlString );

			if ( options.getValueHandlers() == null ) {
				options.getResultMetadataValidator().validate( hqlQuery.getReturnTypes() );
			}

			// determine if we need a result transformer
			List tupleElements = Tuple.class.equals( resultClass )
					? ( ( CompoundSelectionImpl<Tuple> ) selection ).getCompoundSelectionItems()
					: null;
			if ( options.getValueHandlers() != null || tupleElements != null ) {
				hqlQuery.setResultTransformer(
						new CriteriaQueryTransformer( options.getValueHandlers(), tupleElements )
				);
			}
			return new QueryImpl<T>( hqlQuery, this, options.getNamedParameterExplicitTypes() );
		}
		catch ( HibernateException he ) {
			throw convert( he );
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
				return ( X ) get( alias );
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
				return ( X ) get( i );
			}

			public Object[] toArray() {
				return tuples;
			}

			public List<TupleElement<?>> getElements() {
				return tupleElements;
			}
		}
	}

	private CriteriaQueryCompiler criteriaQueryCompiler;

	public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
		if ( criteriaQueryCompiler == null ) {
			criteriaQueryCompiler = new CriteriaQueryCompiler( this );
		}
		return criteriaQueryCompiler.compile( criteriaQuery );
	}

	public Query createNamedQuery(String name) {
		try {
			org.hibernate.Query namedQuery = getSession().getNamedQuery( name );
			try {
				return new QueryImpl( namedQuery, this );
			}
			catch ( HibernateException he ) {
				throw convert( he );
			}
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( "Named query not found: " + name );
		}
	}

	public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
		try {
			/*
			 * Get the named query.
			 * If the named query is a SQL query, get the expected returned type from the query definition
			 * or its associated result set mapping
			 * If the named query is a HQL query, use getReturnType()
			 */
			org.hibernate.Query namedQuery = getSession().getNamedQuery( name );
			//TODO clean this up to avoid downcasting
			final SessionFactoryImplementor factoryImplementor = entityManagerFactory.getSessionFactory();
			final NamedSQLQueryDefinition queryDefinition = factoryImplementor.getNamedSQLQuery( name );
			try {
				if ( queryDefinition != null ) {
					Class<?> actualReturnedClass;

					final NativeSQLQueryReturn[] queryReturns;
					if ( queryDefinition.getQueryReturns() != null ) {
						queryReturns = queryDefinition.getQueryReturns();
					}
					else if ( queryDefinition.getResultSetRef() != null ) {
						final ResultSetMappingDefinition rsMapping = factoryImplementor.getResultSetMapping(
								queryDefinition.getResultSetRef()
						);
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
						final String entityClassName = ( ( NativeSQLQueryRootReturn ) nativeSQLQueryReturn ).getReturnEntityName();
						try {
							actualReturnedClass = ReflectHelper.classForName( entityClassName, AbstractEntityManagerImpl.class );
						}
						catch ( ClassNotFoundException e ) {
							throw new AssertionFailure( "Unable to instantiate class declared on named native query: " + name + " " + entityClassName );
						}
						if ( !resultClass.isAssignableFrom( actualReturnedClass ) ) {
							throw buildIncompatibleException( resultClass, actualReturnedClass );
						}
					}
					else {
						//TODO support other NativeSQLQueryReturn type. For now let it go.
					}
				}
				else {
					resultClassChecking( resultClass, namedQuery );

				}
				return new QueryImpl<T>( namedQuery, this );
			}
			catch ( HibernateException he ) {
				throw convert( he );
			}
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( "Named query not found: " + name );
		}
	}

	private <T> void resultClassChecking(Class<T> resultClass, org.hibernate.Query query) {
		// make sure the query is a select -> HHH-7192
		final SessionImplementor session = unwrap( SessionImplementor.class );
		final HQLQueryPlan queryPlan = session.getFactory().getQueryPlanCache().getHQLQueryPlan(
				query.getQueryString(),
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
			TupleBuilderTransformer tupleTransformer = new TupleBuilderTransformer( query );
			query.setResultTransformer( tupleTransformer );
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
			else if ( query.getReturnTypes().length == 1 ) {
				// if we have only a single return expression, its java type should match with the requested type
				if ( !resultClass.isAssignableFrom( query.getReturnTypes()[0].getReturnedClass() ) ) {
					throw new IllegalArgumentException(
							"Type specified for TypedQuery [" +
									resultClass.getName() +
									"] is incompatible with query return type [" +
									query.getReturnTypes()[0].getReturnedClass() + "]"
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

	private IllegalArgumentException buildIncompatibleException(Class<?> resultClass, Class<?> actualResultClass) {
		return new IllegalArgumentException(
							"Type specified for TypedQuery [" +
									resultClass.getName() +
									"] is incompatible with query return type [" +
									actualResultClass + "]"
					);
	}

	public Query createNativeQuery(String sqlString) {
		try {
			SQLQuery q = getSession().createSQLQuery( sqlString );
			return new QueryImpl( q, this );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	public Query createNativeQuery(String sqlString, Class resultClass) {
		try {
			SQLQuery q = getSession().createSQLQuery( sqlString );
			q.addEntity( "alias1", resultClass.getName(), LockMode.READ );
			return new QueryImpl( q, this );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	public Query createNativeQuery(String sqlString, String resultSetMapping) {
		try {
			SQLQuery q = getSession().createSQLQuery( sqlString );
			q.setResultSetMapping( resultSetMapping );
			return new QueryImpl( q, this );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getReference(Class<T> entityClass, Object primaryKey) {
		try {
			return ( T ) getSession().load( entityClass, ( Serializable ) primaryKey );
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( TypeMismatchException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( ClassCastException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	@SuppressWarnings("unchecked")
	public <A> A find(Class<A> entityClass, Object primaryKey) {
		return find( entityClass, primaryKey, null, null );
	}

	public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
		return find( entityClass, primaryKey, null, properties );
	}

	@SuppressWarnings("unchecked")
	public <A> A find(Class<A> entityClass, Object primaryKey, LockModeType lockModeType) {
		return find( entityClass, primaryKey, lockModeType, null );
	}

	public <A> A find(Class<A> entityClass, Object primaryKey, LockModeType lockModeType, Map<String, Object> properties) {
		CacheMode previousCacheMode = getSession().getCacheMode();
		CacheMode cacheMode = determineAppropriateLocalCacheMode( properties );
		LockOptions lockOptions = null;
		try {
			getSession().setCacheMode( cacheMode );
			if ( lockModeType != null ) {
				lockOptions = getLockRequest( lockModeType, properties );
				return ( A ) getSession().get(
						entityClass, ( Serializable ) primaryKey, 
						lockOptions
				);
			}
			else {
				return ( A ) getSession().get( entityClass, ( Serializable ) primaryKey );
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
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( TypeMismatchException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( ClassCastException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( HibernateException he ) {
			throw convert( he, lockOptions );
		}
		finally {
			getSession().setCacheMode( previousCacheMode );
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
		if ( persistenceContextType == PersistenceContextType.TRANSACTION && !isTransactionInProgress() ) {
			//no need to mark as rollback, no tx in progress
			throw new TransactionRequiredException(
					"no transaction is in progress for a TRANSACTION type persistence context"
			);
		}
	}

	public void persist(Object entity) {
		checkTransactionNeeded();
		try {
			getSession().persist( entity );
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( e.getMessage() );
		}
		catch ( RuntimeException e ) {
			throw convert( e );
		}
	}

	@SuppressWarnings("unchecked")
	public <A> A merge(A entity) {
		checkTransactionNeeded();
		try {
			return ( A ) getSession().merge( entity );
		}
		catch ( ObjectDeletedException sse ) {
			throw new IllegalArgumentException( sse );
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( RuntimeException e ) { //including HibernateException
			throw convert( e );
		}
	}

	public void remove(Object entity) {
		checkTransactionNeeded();
		try {
			getSession().delete( entity );
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( RuntimeException e ) { //including HibernateException
			throw convert( e );
		}
	}

	public void refresh(Object entity) {
		refresh( entity, null, null );
	}

	public void refresh(Object entity, Map<String, Object> properties) {
		refresh( entity, null, properties );
	}

	public void refresh(Object entity, LockModeType lockModeType) {
		refresh( entity, lockModeType, null );
	}

	public void refresh(Object entity, LockModeType lockModeType, Map<String, Object> properties) {
		checkTransactionNeeded();
		CacheMode previousCacheMode = getSession().getCacheMode();
		CacheMode localCacheMode = determineAppropriateLocalCacheMode( properties );
		LockOptions lockOptions = null;
		try {
			getSession().setCacheMode( localCacheMode );
			if ( !getSession().contains( entity ) ) {
				throw new IllegalArgumentException( "Entity not managed" );
			}
			if ( lockModeType != null ) {
				lockOptions = getLockRequest( lockModeType, properties );
				getSession().refresh( entity, lockOptions );
			}
			else {
				getSession().refresh( entity );
			}
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( HibernateException he ) {
			throw convert( he, lockOptions );
		}
		finally {
			getSession().setCacheMode( previousCacheMode );
		}
	}

	public boolean contains(Object entity) {
		try {
			if ( entity != null
					&& !( entity instanceof HibernateProxy )
					&& getSession().getSessionFactory().getClassMetadata( entity.getClass() ) == null ) {
				throw new IllegalArgumentException( "Not an entity:" + entity.getClass() );
			}
			return getSession().contains( entity );
		}
		catch ( MappingException e ) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	public LockModeType getLockMode(Object entity) {
		if ( !contains( entity ) ) {
			throw new IllegalArgumentException( "entity not in the persistence context" );
		}
		return getLockModeType( getSession().getCurrentLockMode( entity ) );
	}

	public void setProperty(String s, Object o) {
		if ( entityManagerSpecificProperties.contains( s ) ) {
			properties.put( s, o );
			applyProperties();
        } else LOG.debugf("Trying to set a property which is not supported on entity manager level");
	}

	public Map<String, Object> getProperties() {
		return Collections.unmodifiableMap( properties );
	}

	public void flush() {
		if ( !isTransactionInProgress() ) {
			throw new TransactionRequiredException( "no transaction is in progress" );
		}
		try {
			getSession().flush();
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
	 */
	protected abstract Session getRawSession();

	public EntityTransaction getTransaction() {
		if ( transactionType == PersistenceUnitTransactionType.JTA ) {
			throw new IllegalStateException( "A JTA EntityManager cannot use getTransaction()" );
		}
		return tx;
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityManagerFactoryImpl getEntityManagerFactory() {
		return entityManagerFactory;
	}

	/**
	 * {@inheritDoc}
	 */
	public HibernateEntityManagerFactory getFactory() {
		return entityManagerFactory;
	}

	/**
	 * {@inheritDoc}
	 */
	public CriteriaBuilder getCriteriaBuilder() {
		return getEntityManagerFactory().getCriteriaBuilder();
	}

	/**
	 * {@inheritDoc}
	 */
	public Metamodel getMetamodel() {
		return getEntityManagerFactory().getMetamodel();
	}

	public void setFlushMode(FlushModeType flushModeType) {
		if ( flushModeType == FlushModeType.AUTO ) {
			getSession().setFlushMode( FlushMode.AUTO );
		}
		else if ( flushModeType == FlushModeType.COMMIT ) {
			getSession().setFlushMode( FlushMode.COMMIT );
		}
		else {
			throw new AssertionFailure( "Unknown FlushModeType: " + flushModeType );
		}
	}

	public void clear() {
		try {
			getSession().clear();
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	public void detach(Object entity) {
		try {
			getSession().evict( entity );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	/**
	 * Hibernate can be set in various flush modes that are unknown to
	 * JPA 2.0. This method can then return null.
	 * If it returns null, do em.unwrap(Session.class).getFlushMode() to get the
	 * Hibernate flush mode
	 */
	public FlushModeType getFlushMode() {
		FlushMode mode = getSession().getFlushMode();
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
		LockOptions lockOptions = null;
		if ( !isTransactionInProgress() ) {
			throw new TransactionRequiredException( "no transaction is in progress" );
		}

		try {
			if ( !contains( entity ) ) {
				throw new IllegalArgumentException( "entity not in the persistence context" );
			}
			lockOptions = getLockRequest( lockModeType, properties );
			getSession().buildLockRequest( lockOptions ).lock( entity );
		}
		catch ( HibernateException he ) {
			throw convert( he, lockOptions );
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
		return ( ( SessionImplementor ) getRawSession() ).isTransactionInProgress();
	}

	private SessionFactoryImplementor sfi() {
		return (SessionFactoryImplementor) getRawSession().getSessionFactory();
	}

	protected void markAsRollback() {
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
							"Using a JTA persistence context wo setting hibernate.transaction.manager_lookup_class"
					);
				}
				try {
                    if ( transactionManager.getStatus() != Status.STATUS_NO_TRANSACTION ) {
                        transactionManager.setRollbackOnly();
                    }
				}
				catch ( SystemException e ) {
					throw new PersistenceException( "Unable to set the JTA transaction as RollbackOnly", e );
				}
			}
		}
	}

	public void joinTransaction() {
		if( !isOpen() ){
			throw new IllegalStateException( "EntityManager is closed" );
		}
		joinTransaction( true );
	}

	public <T> T unwrap(Class<T> clazz) {
		if ( Session.class.isAssignableFrom( clazz ) ) {
			return ( T ) getSession();
		}
		if ( SessionImplementor.class.isAssignableFrom( clazz ) ) {
			return ( T ) getSession();
		}
		if ( EntityManager.class.isAssignableFrom( clazz ) ) {
			return ( T ) this;
		}
		throw new PersistenceException( "Hibernate cannot unwrap " + clazz );
	}

	private void joinTransaction(boolean explicitRequest) {
		if ( transactionType != PersistenceUnitTransactionType.JTA ) {
			if ( explicitRequest ) {
			    LOG.callingJoinTransactionOnNonJtaEntityManager();
			}
			return;
		}

		final SessionImplementor session = (SessionImplementor) getSession();
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
			SynchronizationCallbackCoordinator callbackCoordinator = transactionCoordinator.getSynchronizationCallbackCoordinator();
			callbackCoordinator.setManagedFlushChecker( new ManagedFlushCheckerImpl() );
			callbackCoordinator.setExceptionMapper( new CallbackExceptionMapperImpl() );
			callbackCoordinator.setAfterCompletionAction( new AfterCompletionActionImpl( session, transactionType ) );
		}
		catch ( HibernateException he ) {
			throw convert( he );
		}
	}

	/**
	 * returns the underlying session
	 */
	public Object getDelegate() {
		return getSession();
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		tx = new TransactionImpl( this );
	}

	/**
	 * {@inheritDoc}
	 */
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
			markAsRollback();
		}
		catch ( Exception ne ) {
			//we do not want the subsequent exception to swallow the original one
            LOG.unableToMarkForRollbackOnPersistenceException(ne);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void throwPersistenceException(PersistenceException e) {
		handlePersistenceException( e );
		throw e;
	}

	/**
	 * {@inheritDoc}
	 */
	//FIXME should we remove all calls to this method and use convert(RuntimeException) ?
	public RuntimeException convert(HibernateException e) {
		return convert( e, null );
	}

	public RuntimeException convert(RuntimeException e) {
		RuntimeException result = e;
		if ( e instanceof HibernateException ) {
			result = convert( ( HibernateException ) e );
		}
		else {
			markAsRollback();
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public RuntimeException convert(HibernateException e, LockOptions lockOptions) {
		if ( e instanceof StaleStateException ) {
			PersistenceException converted = wrapStaleStateException( ( StaleStateException ) e );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof LockingStrategyException ) {
			PersistenceException converted = wrapLockException( e, lockOptions );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof org.hibernate.exception.LockTimeoutException ) {
			PersistenceException converted = wrapLockException( e, lockOptions );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof org.hibernate.PessimisticLockException ) {
			PersistenceException converted = wrapLockException( e, lockOptions );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof org.hibernate.QueryTimeoutException ) {
			QueryTimeoutException converted = new QueryTimeoutException(e.getMessage(), e);
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof ObjectNotFoundException ) {
			EntityNotFoundException converted = new EntityNotFoundException( e.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
        else if ( e instanceof org.hibernate.NonUniqueObjectException ) {
            EntityExistsException converted = new EntityExistsException( e.getMessage() );
            handlePersistenceException( converted );
            return converted;
        }
		else if ( e instanceof org.hibernate.NonUniqueResultException ) {
			NonUniqueResultException converted = new NonUniqueResultException( e.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof UnresolvableObjectException ) {
			EntityNotFoundException converted = new EntityNotFoundException( e.getMessage() );
			handlePersistenceException( converted );
			return converted;
		}
		else if ( e instanceof QueryException ) {
			return new IllegalArgumentException( e );
		}
		else if ( e instanceof TransientObjectException ) {
			try {
				markAsRollback();
			}
			catch ( Exception ne ) {
				//we do not want the subsequent exception to swallow the original one
                LOG.unableToMarkForRollbackOnTransientObjectException(ne);
			}
			return new IllegalStateException( e ); //Spec 3.2.3 Synchronization rules
		}
		else {
			PersistenceException converted = new PersistenceException( e );
			handlePersistenceException( converted );
			return converted;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void throwPersistenceException(HibernateException e) {
		throw convert( e );
	}

	/**
	 * {@inheritDoc}
	 */
	public PersistenceException wrapStaleStateException(StaleStateException e) {
		PersistenceException pe;
		if ( e instanceof StaleObjectStateException ) {
			StaleObjectStateException sose = ( StaleObjectStateException ) e;
			Serializable identifier = sose.getIdentifier();
			if ( identifier != null ) {
				try {
					Object entity = getRawSession().load( sose.getEntityName(), identifier );
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
			PessimisticEntityLockException lockException = (PessimisticEntityLockException) e;
			if ( lockOptions != null && lockOptions.getTimeOut() > -1 ) {
				// assume lock timeout occurred if a timeout or NO WAIT was specified
				pe = new LockTimeoutException( lockException.getMessage(), lockException, lockException.getEntity() );
			}
			else {
				pe = new PessimisticLockException( lockException.getMessage(), lockException, lockException.getEntity() );
			}
		}
		else if ( e instanceof org.hibernate.PessimisticLockException ) {
			org.hibernate.PessimisticLockException jdbcLockException = ( org.hibernate.PessimisticLockException ) e;
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
			return ! coordinator.getTransactionContext().isClosed() &&
					! coordinator.getTransactionContext().isFlushModeNever() &&
					! JtaStatusHelper.isRollback( jtaStatus );
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
