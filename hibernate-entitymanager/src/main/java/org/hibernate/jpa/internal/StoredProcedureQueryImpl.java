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
package org.hibernate.jpa.internal;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.ParameterMode;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TemporalType;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureResult;
import org.hibernate.result.ResultSetReturn;
import org.hibernate.result.Return;
import org.hibernate.result.UpdateCountReturn;
import org.hibernate.jpa.spi.BaseQueryImpl;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;

/**
 * @author Steve Ebersole
 */
public class StoredProcedureQueryImpl extends BaseQueryImpl implements StoredProcedureQuery {
	private final ProcedureCall procedureCall;
	private ProcedureResult procedureResult;

	public StoredProcedureQueryImpl(ProcedureCall procedureCall, HibernateEntityManagerImplementor entityManager) {
		super( entityManager );
		this.procedureCall = procedureCall;
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
		procedureCall.registerParameter( position, type, mode );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public StoredProcedureQuery registerStoredProcedureParameter(String parameterName, Class type, ParameterMode mode) {
		entityManager().checkOpen( true );
		procedureCall.registerParameter( parameterName, type, mode );
		return this;
	}

	@Override
	protected void validateParameterBindingTypes(ParameterImplementor parameter, ParameterValue bindValue) {
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


	// outputs ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private ProcedureResult outputs() {
		if ( procedureResult == null ) {
			procedureResult = procedureCall.getResult();
		}
		return procedureResult;
	}

	@Override
	public Object getOutputParameterValue(int position) {
		return outputs().getOutputParameterValue( position );
	}

	@Override
	public Object getOutputParameterValue(String parameterName) {
		return outputs().getOutputParameterValue( parameterName );
	}

	@Override
	public boolean execute() {
		return outputs().hasMoreReturns();
	}

	@Override
	public int executeUpdate() {
		return getUpdateCount();
	}

	@Override
	public boolean hasMoreResults() {
		return outputs().hasMoreReturns();
	}

	@Override
	public int getUpdateCount() {
		final Return nextReturn = outputs().getNextReturn();
		if ( nextReturn.isResultSet() ) {
			return -1;
		}
		return ( (UpdateCountReturn) nextReturn ).getUpdateCount();
	}

	@Override
	public List getResultList() {
		final Return nextReturn = outputs().getNextReturn();
		if ( ! nextReturn.isResultSet() ) {
			return null; // todo : what should be thrown/returned here?
		}
		return ( (ResultSetReturn) nextReturn ).getResultList();
	}

	@Override
	public Object getSingleResult() {
		final Return nextReturn = outputs().getNextReturn();
		if ( ! nextReturn.isResultSet() ) {
			return null; // todo : what should be thrown/returned here?
		}
		return ( (ResultSetReturn) nextReturn ).getSingleResult();
	}

	@Override
	public <T> T unwrap(Class<T> cls) {
		return null;
	}


	// ugh ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public Query setLockMode(LockModeType lockMode) {
		return null;
	}

	@Override
	public LockModeType getLockMode() {
		return null;
	}


	// unsupported hints/calls ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	protected void applyFirstResult(int firstResult) {
	}

	@Override
	protected void applyMaxResults(int maxResults) {
	}

	@Override
	protected boolean canApplyLockModesHints() {
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
	protected boolean applyFetchSize(int fetchSize) {
		return false;
	}
}
