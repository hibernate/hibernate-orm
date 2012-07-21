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
import org.hibernate.StoredProcedureCall;
import org.hibernate.StoredProcedureOutputs;
import org.hibernate.StoredProcedureResultSetReturn;
import org.hibernate.StoredProcedureReturn;
import org.hibernate.StoredProcedureUpdateCountReturn;
import org.hibernate.jpa.spi.BaseQueryImpl;
import org.hibernate.jpa.spi.HibernateEntityManagerImplementor;

/**
 * @author Steve Ebersole
 */
public class StoredProcedureQueryImpl extends BaseQueryImpl implements StoredProcedureQuery {
	private final StoredProcedureCall storedProcedureCall;
	private StoredProcedureOutputs storedProcedureOutputs;

	public StoredProcedureQueryImpl(StoredProcedureCall storedProcedureCall, HibernateEntityManagerImplementor entityManager) {
		super( entityManager );
		this.storedProcedureCall = storedProcedureCall;
	}

	@Override
	protected boolean applyTimeoutHint(int timeout) {
		storedProcedureCall.setTimeout( timeout );
		return true;
	}

	@Override
	protected boolean applyCacheableHint(boolean isCacheable) {
		storedProcedureCall.setCacheable( isCacheable );
		return true;
	}

	@Override
	protected boolean applyCacheRegionHint(String regionName) {
		storedProcedureCall.setCacheRegion( regionName );
		return true;
	}

	@Override
	protected boolean applyReadOnlyHint(boolean isReadOnly) {
		storedProcedureCall.setReadOnly( isReadOnly );
		return true;
	}

	@Override
	protected boolean applyCacheModeHint(CacheMode cacheMode) {
		storedProcedureCall.setCacheMode( cacheMode );
		return true;
	}

	@Override
	protected boolean applyFlushModeHint(FlushMode flushMode) {
		storedProcedureCall.setFlushMode( flushMode );
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public StoredProcedureQuery registerStoredProcedureParameter(int position, Class type, ParameterMode mode) {
		storedProcedureCall.registerStoredProcedureParameter( position, type, mode );
		return this;
	}

	@Override
	public StoredProcedureQuery registerStoredProcedureParameter(String parameterName, Class type, ParameterMode mode) {
		storedProcedureCall.registerStoredProcedureParameter( parameterName, type, mode );
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

	private StoredProcedureOutputs outputs() {
		if ( storedProcedureOutputs == null ) {
			storedProcedureOutputs = storedProcedureCall.getOutputs();
		}
		return storedProcedureOutputs;
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
		final StoredProcedureReturn nextReturn = outputs().getNextReturn();
		if ( nextReturn.isResultSet() ) {
			return -1;
		}
		return ( (StoredProcedureUpdateCountReturn) nextReturn ).getUpdateCount();
	}

	@Override
	public List getResultList() {
		final StoredProcedureReturn nextReturn = outputs().getNextReturn();
		if ( ! nextReturn.isResultSet() ) {
			return null; // todo : what should be thrown/returned here?
		}
		return ( (StoredProcedureResultSetReturn) nextReturn ).getResultList();
	}

	@Override
	public Object getSingleResult() {
		final StoredProcedureReturn nextReturn = outputs().getNextReturn();
		if ( ! nextReturn.isResultSet() ) {
			return null; // todo : what should be thrown/returned here?
		}
		return ( (StoredProcedureResultSetReturn) nextReturn ).getSingleResult();
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
