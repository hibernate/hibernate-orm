/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.PersistenceContextType;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.AbstractSessionImpl;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.QueryHints;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.UnknownSqlResultSetMappingException;
import org.hibernate.query.procedure.internal.StoredProcedureQueryImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import static org.hibernate.internal.HEMLogging.messageLogger;

/**
 * @author <a href="mailto:gavin@hibernate.org">Gavin King</a>
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public abstract class AbstractEntityManagerImpl
		extends AbstractSessionImpl
		implements HibernateEntityManagerImplementor, Serializable {
	private static final long serialVersionUID = 78818181L;

	private static final EntityManagerMessageLogger LOG = messageLogger( AbstractEntityManagerImpl.class );

	private static final List<String> ENTITY_MANAGER_SPECIFIC_PROPERTIES = new ArrayList<>();

	static {
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( AvailableSettings.LOCK_SCOPE );
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( AvailableSettings.LOCK_TIMEOUT );
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( AvailableSettings.FLUSH_MODE );
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( AvailableSettings.SHARED_CACHE_RETRIEVE_MODE );
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( AvailableSettings.SHARED_CACHE_STORE_MODE );
		ENTITY_MANAGER_SPECIFIC_PROPERTIES.add( QueryHints.SPEC_HINT_TIMEOUT );
	}

	private SessionFactoryImpl entityManagerFactory;
	private SynchronizationType synchronizationType;
	private PersistenceUnitTransactionType transactionType;
	private Map<String, Object> properties;
	private LockOptions lockOptions;

	protected AbstractEntityManagerImpl(
			SessionFactoryImpl entityManagerFactory,
			PersistenceContextType type,  // TODO:  remove as no longer used
			SynchronizationType synchronizationType,
			PersistenceUnitTransactionType transactionType,
			Map properties) {
		super( entityManagerFactory, null ); // null -> "tenant identifier"
		this.entityManagerFactory = entityManagerFactory;
		this.synchronizationType = synchronizationType;
		this.transactionType = transactionType;

		this.lockOptions = new LockOptions();
		this.properties = new HashMap<>();
		for ( String key : ENTITY_MANAGER_SPECIFIC_PROPERTIES ) {
			if ( entityManagerFactory.getProperties().containsKey( key ) ) {
				this.properties.put( key, entityManagerFactory.getProperties().get( key ) );
			}
			if ( properties != null && properties.containsKey( key ) ) {
				this.properties.put( key, properties.get( key ) );
			}
		}
	}

	@Override
	public SessionFactoryImplementor getEntityManagerFactory() {
		return entityManagerFactory;
	}

	public PersistenceUnitTransactionType getTransactionType() {
		return transactionType;
	}

	@Override
	public SynchronizationType getSynchronizationType() {
		return synchronizationType;
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder() {
		return entityManagerFactory.getCriteriaBuilder();
	}

	@Override
	public Metamodel getMetamodel() {
		return entityManagerFactory.getMetamodel();
	}

	@Override
	public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
		checkOpen();
		try {
			final ProcedureCallMemento memento = getFactory().getNamedQueryRepository().getNamedProcedureCallMemento( name );
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
	public RuntimeException convert(HibernateException e) {
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
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
		checkOpen();
		try {
			return new StoredProcedureQueryImpl(
					createStoredProcedureCall( procedureName ),
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
					createStoredProcedureCall( procedureName, resultClasses ),
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
						createStoredProcedureCall( procedureName, resultSetMappings ),
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
	public boolean isConnected() {
		checkTransactionSynchStatus();
		return !isClosed() && getJdbcCoordinator().getLogicalConnection().isOpen();
	}

	@Override
	public boolean isTransactionInProgress() {
		checkTransactionSynchStatus();
		return !isClosed() && getTransactionCoordinator().getTransactionDriverControl()
				.getStatus() == TransactionStatus.ACTIVE && getTransactionCoordinator().isJoined();
	}

	private void checkTransactionSynchStatus() {
		pulseTransactionCoordinator();
		delayedAfterCompletion();
	}

	private void pulseTransactionCoordinator() {
		if ( !isClosed() ) {
			getTransactionCoordinator().pulse();
		}
	}

	private void delayedAfterCompletion() {
		if ( getTransactionCoordinator() instanceof JtaTransactionCoordinatorImpl ) {
			( (JtaTransactionCoordinatorImpl) getTransactionCoordinator() ).getSynchronizationCallbackCoordinator()
					.processAnyDelayedAfterCompletion();
		}
	}
}
