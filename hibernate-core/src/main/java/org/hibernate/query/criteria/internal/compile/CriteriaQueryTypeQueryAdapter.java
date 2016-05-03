/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.compile;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.TemporalType;
import javax.persistence.criteria.ParameterExpression;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.Query;
import org.hibernate.query.internal.AbstractProducedQuery;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.type.Type;

/**
 * <strong>Make this go away in 6.0</strong> :)
 * <p/>
 * Needed because atm we render a JPA Criteria query into a HQL/JPQL query String and some metadata, and then
 * compile into a Query.  This class wraps the compiled HQL/JPQL query and adds an extra layer of metadata.
 * <p/>
 * But the move to SQM in 6.0 allows us to do away with the "wrapping".
 *
 * Essentially
 *
 * @author Steve Ebersole
 */
public class CriteriaQueryTypeQueryAdapter<X> extends AbstractProducedQuery<X> implements QueryImplementor<X> {
	private final SessionImplementor entityManager;
	private final QueryImplementor<X> jpqlQuery;
	private final Map<ParameterExpression<?>, ExplicitParameterInfo<?>> explicitParameterInfoMap;

	public CriteriaQueryTypeQueryAdapter(
			SessionImplementor entityManager,
			QueryImplementor<X> jpqlQuery,
			Map<ParameterExpression<?>, ExplicitParameterInfo<?>> explicitParameterInfoMap) {
		super( entityManager, jpqlQuery.getParameterMetadata() );
		this.entityManager = entityManager;
		this.jpqlQuery = jpqlQuery;
		this.explicitParameterInfoMap = explicitParameterInfoMap;
	}

	public List<X> getResultList() {
		return jpqlQuery.getResultList();
	}

	public X getSingleResult() {
		return jpqlQuery.getSingleResult();
	}

	public int getMaxResults() {
		return jpqlQuery.getMaxResults();
	}

	public QueryImplementor<X> setMaxResults(int i) {
		jpqlQuery.setMaxResults( i );
		return this;
	}

	public int getFirstResult() {
		return jpqlQuery.getFirstResult();
	}

	public QueryImplementor<X> setFirstResult(int i) {
		jpqlQuery.setFirstResult( i );
		return this;
	}

	public Map<String, Object> getHints() {
		return jpqlQuery.getHints();
	}

	public QueryImplementor<X> setHint(String name, Object value) {
		jpqlQuery.setHint( name, value );
		return this;
	}

	@Override
	protected boolean isNativeQuery() {
		return false;
	}

	@Override
	public String getQueryString() {
		return jpqlQuery.getQueryString();
	}

	public FlushModeType getFlushMode() {
		return jpqlQuery.getFlushMode();
	}

	@Override
	public Type[] getReturnTypes() {
		return jpqlQuery.getReturnTypes();
	}

	public QueryImplementor<X> setFlushMode(FlushModeType flushModeType) {
		jpqlQuery.setFlushMode( flushModeType );
		return this;
	}

	public LockModeType getLockMode() {
		return jpqlQuery.getLockMode();
	}

	public QueryImplementor<X> setLockMode(LockModeType lockModeType) {
		jpqlQuery.setLockMode( lockModeType );
		return this;
	}

	@Override
	public Query<X> setEntity(int position, Object val) {
		return null;
	}

	@Override
	public Query<X> setEntity(String name, Object val) {
		return null;
	}

	@Override
	public String[] getReturnAliases() {
		return new String[0];
	}

	@SuppressWarnings({ "unchecked" })
	public Set<Parameter<?>> getParameters() {
		entityManager.checkOpen( false );
		return new HashSet( explicitParameterInfoMap.values() );
	}

	public boolean isBound(Parameter<?> param) {
		entityManager.checkOpen( false );
		return jpqlQuery.isBound( param );
	}

	@SuppressWarnings({ "unchecked" })
	public <T> T getParameterValue(Parameter<T> param) {
		entityManager.checkOpen( false );
		final ExplicitParameterInfo parameterInfo = resolveParameterInfo( param );
		if ( parameterInfo.isNamed() ) {
			return ( T ) jpqlQuery.getParameterValue( parameterInfo.getName() );
		}
		else {
			return ( T ) jpqlQuery.getParameterValue( parameterInfo.getPosition() );
		}
	}

	private <T> ExplicitParameterInfo resolveParameterInfo(Parameter<T> param) {
		if ( ExplicitParameterInfo.class.isInstance( param ) ) {
			return (ExplicitParameterInfo) param;
		}
		else if ( ParameterExpression.class.isInstance( param ) ) {
			return explicitParameterInfoMap.get( (ParameterExpression) param );
		}
		else {
			for ( ExplicitParameterInfo parameterInfo : explicitParameterInfoMap.values() ) {
				if ( param.getName() != null && param.getName().equals( parameterInfo.getName() ) ) {
					return parameterInfo;
				}
				else if ( param.getPosition() != null && param.getPosition().equals( parameterInfo.getPosition() ) ) {
					return parameterInfo;
				}
			}
		}
		throw new IllegalArgumentException( "Unable to locate parameter [" + param + "] in query" );
	}

	@SuppressWarnings({ "unchecked" })
	public <T> QueryImplementor<X> setParameter(Parameter<T> param, T t) {
		entityManager.checkOpen( false );
		final ExplicitParameterInfo parameterInfo = resolveParameterInfo( param );
		if ( parameterInfo.isNamed() ) {
			jpqlQuery.setParameter( parameterInfo.getName(), t );
		}
		else {
			jpqlQuery.setParameter( parameterInfo.getPosition(), t );
		}
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public QueryImplementor<X> setParameter(Parameter<Calendar> param, Calendar calendar, TemporalType temporalType) {
		entityManager.checkOpen( false );
		final ExplicitParameterInfo parameterInfo = resolveParameterInfo( param );
		if ( parameterInfo.isNamed() ) {
			jpqlQuery.setParameter( parameterInfo.getName(), calendar, temporalType );
		}
		else {
			jpqlQuery.setParameter( parameterInfo.getPosition(), calendar, temporalType );
		}
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public QueryImplementor<X> setParameter(Parameter<Date> param, Date date, TemporalType temporalType) {
		entityManager.checkOpen( false );
		final ExplicitParameterInfo parameterInfo = resolveParameterInfo( param );
		if ( parameterInfo.isNamed() ) {
			jpqlQuery.setParameter( parameterInfo.getName(), date, temporalType );
		}
		else {
			jpqlQuery.setParameter( parameterInfo.getPosition(), date, temporalType );
		}
		return this;
	}

	public <T> T unwrap(Class<T> cls) {
		return jpqlQuery.unwrap( cls );
	}

	@SuppressWarnings({ "unchecked" })
	public Object getParameterValue(String name) {
		entityManager.checkOpen( false );
		locateParameterByName( name );
		return jpqlQuery.getParameterValue( name );
	}

	private ExplicitParameterInfo locateParameterByName(String name) {
		for ( ExplicitParameterInfo parameterInfo : explicitParameterInfoMap.values() ) {
			if ( parameterInfo.isNamed() && parameterInfo.getName().equals( name ) ) {
				return parameterInfo;
			}
		}
		throw new IllegalArgumentException( "Unable to locate parameter registered with that name [" + name + "]" );
	}

	public Parameter<?> getParameter(String name) {
		entityManager.checkOpen( false );
		return locateParameterByName( name );
	}

	@SuppressWarnings({ "unchecked" })
	public <T> Parameter<T> getParameter(String name, Class<T> type) {
		entityManager.checkOpen( false );
		Parameter parameter = locateParameterByName( name );
		if ( type.isAssignableFrom( parameter.getParameterType() ) ) {
			return parameter;
		}
		throw new IllegalArgumentException(
				"Named parameter [" + name + "] type is not assignanle to request type ["
						+ type.getName() + "]"
		);
	}

	@SuppressWarnings({ "unchecked" })
	public QueryImplementor<X> setParameter(String name, Object value) {
		entityManager.checkOpen( true );
		ExplicitParameterInfo parameterInfo = locateParameterByName( name );
		parameterInfo.validateBindValue( value );
		jpqlQuery.setParameter( name, value );
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public QueryImplementor<X> setParameter(String name, Calendar calendar, TemporalType temporalType) {
		entityManager.checkOpen( true );
		ExplicitParameterInfo parameterInfo = locateParameterByName( name );
		parameterInfo.validateCalendarBind();
		jpqlQuery.setParameter( name, calendar, temporalType );
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public QueryImplementor<X> setParameter(String name, Date date, TemporalType temporalType) {
		entityManager.checkOpen( true );
		ExplicitParameterInfo parameterInfo = locateParameterByName( name );
		parameterInfo.validateDateBind();
		jpqlQuery.setParameter( name, date, temporalType );
		return this;
	}


	// unsupported stuff ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public int executeUpdate() {
		throw new IllegalStateException( "Typed criteria queries do not support executeUpdate" );
	}

	public QueryImplementor<X> setParameter(int i, Object o) {
		throw new IllegalArgumentException( "Criteria queries do not support positioned parameters" );
	}

	public QueryImplementor<X> setParameter(int i, Calendar calendar, TemporalType temporalType) {
		throw new IllegalArgumentException( "Criteria queries do not support positioned parameters" );
	}

	public QueryImplementor<X> setParameter(int i, Date date, TemporalType temporalType) {
		throw new IllegalArgumentException( "Criteria queries do not support positioned parameters" );
	}

	public Object getParameterValue(int position) {
		throw new IllegalArgumentException( "Criteria queries do not support positioned parameters" );
	}

	public Parameter<?> getParameter(int position) {
		throw new IllegalArgumentException( "Criteria queries do not support positioned parameters" );
	}

	public <T> Parameter<T> getParameter(int position, Class<T> type) {
		throw new IllegalArgumentException( "Criteria queries do not support positioned parameters" );
	}
}
