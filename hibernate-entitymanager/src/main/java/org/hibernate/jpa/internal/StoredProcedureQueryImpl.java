/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Parameter;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TemporalType;
import javax.persistence.TransactionRequiredException;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.jpa.spi.BaseQueryImpl;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;
import org.hibernate.jpa.spi.ParameterBind;
import org.hibernate.jpa.spi.ParameterRegistration;
import org.hibernate.procedure.NoSuchParameterException;
import org.hibernate.procedure.ParameterStrategyException;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.result.NoMoreReturnsException;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;
import org.hibernate.result.UpdateCountOutput;

/**
 * @author Steve Ebersole
 */
public class StoredProcedureQueryImpl extends BaseQueryImpl implements StoredProcedureQuery {
	private final ProcedureCall procedureCall;
	private ProcedureOutputs procedureResult;

	public StoredProcedureQueryImpl(ProcedureCall procedureCall, HibernateEntityManagerImplementor entityManager) {
		super( entityManager );
		this.procedureCall = procedureCall;
	}

	/**
	 * This form is used to build a StoredProcedureQueryImpl from a memento (usually from a NamedStoredProcedureQuery).
	 *
	 * @param memento The memento
	 * @param entityManager The EntityManager
	 */
	@SuppressWarnings("unchecked")
	public StoredProcedureQueryImpl(ProcedureCallMemento memento, HibernateEntityManagerImplementor entityManager) {
		super( entityManager );
		this.procedureCall = memento.makeProcedureCall( entityManager.getSession() );
		for ( org.hibernate.procedure.ParameterRegistration nativeParamReg : procedureCall.getRegisteredParameters() ) {
			registerParameter( new ParameterRegistrationImpl( this, nativeParamReg ) );
		}
	}

	@Override
	protected boolean applyTimeoutHint(int timeout) {
		procedureCall.setTimeout( timeout );
		return true;
	}

	@Override
	protected boolean applyCacheableHint(boolean isCacheable) {
		procedureCall.setCacheable( isCacheable );
		return true;
	}

	@Override
	protected boolean applyCacheRegionHint(String regionName) {
		procedureCall.setCacheRegion( regionName );
		return true;
	}

	@Override
	protected boolean applyReadOnlyHint(boolean isReadOnly) {
		procedureCall.setReadOnly( isReadOnly );
		return true;
	}

	@Override
	protected boolean applyCacheModeHint(CacheMode cacheMode) {
		procedureCall.setCacheMode( cacheMode );
		return true;
	}

	@Override
	protected boolean applyFlushModeHint(FlushMode flushMode) {
		procedureCall.setFlushMode( flushMode );
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public StoredProcedureQuery registerStoredProcedureParameter(int position, Class type, ParameterMode mode) {
		entityManager().checkOpen( true );

		try {
			registerParameter(
					new ParameterRegistrationImpl(
							this,
							procedureCall.registerParameter( position, type, mode )
					)
			);
		}
		catch (HibernateException he) {
			throw entityManager().convert( he );
		}
		catch (RuntimeException e) {
			entityManager().markForRollbackOnly();
			throw e;
		}

		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public StoredProcedureQuery registerStoredProcedureParameter(String parameterName, Class type, ParameterMode mode) {
		entityManager().checkOpen( true );

		try {
			registerParameter(
					new ParameterRegistrationImpl(
							this,
							procedureCall.registerParameter( parameterName, type, mode )
					)
			);
		}
		catch (HibernateException he) {
			throw entityManager().convert( he );
		}
		catch (RuntimeException e) {
			entityManager().markForRollbackOnly();
			throw e;
		}

		return this;
	}

	@Override
	public <T> StoredProcedureQueryImpl setParameter(Parameter<T> param, T value) {
		return (StoredProcedureQueryImpl) super.setParameter( param, value );
	}

	@Override
	public StoredProcedureQueryImpl setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		return (StoredProcedureQueryImpl) super.setParameter( param, value, temporalType );
	}

	@Override
	public StoredProcedureQueryImpl setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		return (StoredProcedureQueryImpl) super.setParameter( param, value, temporalType );
	}

	@Override
	public StoredProcedureQueryImpl setParameter(String name, Object value) {
		return ( StoredProcedureQueryImpl) super.setParameter( name, value );
	}

	@Override
	public StoredProcedureQueryImpl setParameter(String name, Calendar value, TemporalType temporalType) {
		return (StoredProcedureQueryImpl) super.setParameter( name, value, temporalType );
	}

	@Override
	public StoredProcedureQueryImpl setParameter(String name, Date value, TemporalType temporalType) {
		return (StoredProcedureQueryImpl) super.setParameter( name, value, temporalType );
	}

	@Override
	public StoredProcedureQueryImpl setParameter(int position, Object value) {
		return (StoredProcedureQueryImpl) super.setParameter( position, value );
	}

	@Override
	public StoredProcedureQueryImpl setParameter(int position, Calendar value, TemporalType temporalType) {
		return (StoredProcedureQueryImpl) super.setParameter( position, value, temporalType );
	}

	@Override
	public StoredProcedureQueryImpl setParameter(int position, Date value, TemporalType temporalType) {
		return (StoredProcedureQueryImpl) super.setParameter( position, value, temporalType );
	}


	// covariant returns ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public StoredProcedureQueryImpl setFlushMode(FlushModeType jpaFlushMode) {
		return (StoredProcedureQueryImpl) super.setFlushMode( jpaFlushMode );
	}

	@Override
	public StoredProcedureQueryImpl setHint(String hintName, Object value) {
		return (StoredProcedureQueryImpl) super.setHint( hintName, value );
	}


	// outputs ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean execute() {
		try {
			final Output rtn = outputs().getCurrent();
			return rtn != null && ResultSetOutput.class.isInstance( rtn );
		}
		catch (NoMoreReturnsException e) {
			return false;
		}
		catch (HibernateException he) {
			throw entityManager().convert( he );
		}
		catch (RuntimeException e) {
			entityManager().markForRollbackOnly();
			throw e;
		}
	}

	protected ProcedureOutputs outputs() {
		if ( procedureResult == null ) {
			procedureResult = procedureCall.getOutputs();
		}
		return procedureResult;
	}

	@Override
	public int executeUpdate() {
		if ( ! entityManager().isTransactionInProgress() ) {
			throw new TransactionRequiredException( "javax.persistence.Query.executeUpdate requires active transaction" );
		}

		// the expectation is that there is just one Output, of type UpdateCountOutput
		try {
			execute();
			return getUpdateCount();
		}
		finally {
			outputs().release();
		}
	}

	@Override
	public Object getOutputParameterValue(int position) {
		// NOTE : according to spec (specifically), an exception thrown from this method should not mark for rollback.
		try {
			return outputs().getOutputParameterValue( position );
		}
		catch (ParameterStrategyException e) {
			throw new IllegalArgumentException( "Invalid mix of named and positional parameters", e );
		}
		catch (NoSuchParameterException e) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
	}

	@Override
	public Object getOutputParameterValue(String parameterName) {
		// NOTE : according to spec (specifically), an exception thrown from this method should not mark for rollback.
		try {
			return outputs().getOutputParameterValue( parameterName );
		}
		catch (ParameterStrategyException e) {
			throw new IllegalArgumentException( "Invalid mix of named and positional parameters", e );
		}
		catch (NoSuchParameterException e) {
			throw new IllegalArgumentException( e.getMessage(), e );
		}
	}

	@Override
	public boolean hasMoreResults() {
		return outputs().goToNext() && ResultSetOutput.class.isInstance( outputs().getCurrent() );
	}

	@Override
	public int getUpdateCount() {
		try {
			final Output rtn = outputs().getCurrent();
			if ( rtn == null ) {
				return -1;
			}
			else if ( UpdateCountOutput.class.isInstance( rtn ) ) {
				return ( (UpdateCountOutput) rtn ).getUpdateCount();
			}
			else {
				return -1;
			}
		}
		catch (NoMoreReturnsException e) {
			return -1;
		}
		catch (HibernateException he) {
			throw entityManager().convert( he );
		}
		catch (RuntimeException e) {
			entityManager().markForRollbackOnly();
			throw e;
		}
	}

	@Override
	public List getResultList() {
		try {
			final Output rtn = outputs().getCurrent();
			if ( ! ResultSetOutput.class.isInstance( rtn ) ) {
				throw new IllegalStateException( "Current CallableStatement ou was not a ResultSet, but getResultList was called" );
			}

			return ( (ResultSetOutput) rtn ).getResultList();
		}
		catch (NoMoreReturnsException e) {
			// todo : the spec is completely silent on these type of edge-case scenarios.
			// Essentially here we'd have a case where there are no more results (ResultSets nor updateCount) but
			// getResultList was called.
			return null;
		}
		catch (HibernateException he) {
			throw entityManager().convert( he );
		}
		catch (RuntimeException e) {
			entityManager().markForRollbackOnly();
			throw e;
		}
	}

	@Override
	public Object getSingleResult() {
		final List resultList = getResultList();
		if ( resultList == null || resultList.isEmpty() ) {
			throw new NoResultException(
					String.format(
							"Call to stored procedure [%s] returned no results",
							procedureCall.getProcedureName()
					)
			);
		}
		else if ( resultList.size() > 1 ) {
			throw new NonUniqueResultException(
					String.format(
							"Call to stored procedure [%s] returned multiple results",
							procedureCall.getProcedureName()
					)
			);
		}

		return resultList.get( 0 );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( ProcedureCall.class.isAssignableFrom( cls ) ) {
			return (T) procedureCall;
		}
		else if ( ProcedureOutputs.class.isAssignableFrom( cls ) ) {
			return (T) outputs();
		}
		else if ( StoredProcedureQueryImpl.class.isAssignableFrom( cls ) ) {
			return (T) this;
		}
		else if ( StoredProcedureQuery.class.equals( cls ) ) {
			return (T) this;
		}

		throw new PersistenceException(
				String.format(
						"Unsure how to unwrap %s impl [%s] as requested type [%s]",
						StoredProcedureQuery.class.getSimpleName(),
						this.getClass().getName(),
						cls.getName()
				)
		);
	}

	@Override
	protected boolean isNativeSqlQuery() {
		return false;
	}

	@Override
	protected boolean isSelectQuery() {
		return false;
	}

	@Override
	public Query setLockMode(LockModeType lockMode) {
		throw new IllegalStateException( "javax.persistence.Query.setLockMode not valid on javax.persistence.StoredProcedureQuery" );
	}

	@Override
	public LockModeType getLockMode() {
		throw new IllegalStateException( "javax.persistence.Query.getLockMode not valid on javax.persistence.StoredProcedureQuery" );
	}


	// unsupported hints/calls ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	@Override
	protected void internalApplyLockMode(LockModeType lockModeType) {
		throw new IllegalStateException( "Specifying LockMode not valid on javax.persistence.StoredProcedureQuery" );
	}

	@Override
	protected void applyFirstResult(int firstResult) {
	}

	@Override
	protected void applyMaxResults(int maxResults) {
	}

	@Override
	protected boolean canApplyAliasSpecificLockModeHints() {
		return false;
	}

	@Override
	protected void applyAliasSpecificLockModeHint(String alias, LockMode lockMode) {
	}

	@Override
	protected boolean applyLockTimeoutHint(int timeout) {
		return false;
	}

	@Override
	protected boolean applyCommentHint(String comment) {
		return false;
	}

	@Override
	protected boolean applyFetchSizeHint(int fetchSize) {
		return false;
	}


	// parameters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public ProcedureCall getHibernateProcedureCall() {
		return procedureCall;
	}

	private static class ParameterRegistrationImpl<T> implements ParameterRegistration<T> {
		private final StoredProcedureQueryImpl query;
		private final org.hibernate.procedure.ParameterRegistration<T> nativeParamRegistration;

		private ParameterBind<T> bind;

		public ParameterRegistrationImpl(
				StoredProcedureQueryImpl query,
				org.hibernate.procedure.ParameterRegistration<T> nativeParamRegistration) {
			this.query = query;
			this.nativeParamRegistration = nativeParamRegistration;
		}

		@Override
		public String getName() {
			return nativeParamRegistration.getName();
		}

		@Override
		public Integer getPosition() {
			return nativeParamRegistration.getPosition();
		}

		@Override
		public Class<T> getParameterType() {
			return nativeParamRegistration.getType();
		}

		@Override
		public boolean isJpaPositionalParameter() {
			return getPosition() != null;
		}

		@Override
		public Query getQuery() {
			return query;
		}

		@Override
		public ParameterMode getMode() {
			return nativeParamRegistration.getMode();
		}

		@Override
		public boolean isBindable() {
			return getMode() == ParameterMode.IN || getMode() == ParameterMode.INOUT;
		}

		@Override
		public void bindValue(T value) {
			bindValue( value, null );
		}

		@Override
		public void bindValue(T value, TemporalType specifiedTemporalType) {
			validateBinding( getParameterType(), value, specifiedTemporalType );

			nativeParamRegistration.bindValue( value,specifiedTemporalType );

			if ( bind == null ) {
				bind = new ParameterBind<T>() {
					@Override
					public T getValue() {
						return nativeParamRegistration.getBind().getValue();
					}

					@Override
					public TemporalType getSpecifiedTemporalType() {
						return nativeParamRegistration.getBind().getExplicitTemporalType();
					}
				};
			}
		}

		@Override
		public ParameterBind<T> getBind() {
			return bind;
		}
	}
}
