/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.procedure.spi;

import java.util.Calendar;
import java.util.Date;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TemporalType;

import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.spi.QueryImplementor;

/**
 * Defines extended support for JPA-defined stored-procedure execution.  This is the one Query form
 * that still uses wrapping as there are very fundamental differences between Hibernate's
 * native procedure support and JPA's.
 *
 * @author Steve Ebersole
 */
public interface StoredProcedureQueryImplementor extends StoredProcedureQuery, QueryImplementor<Object> {
	ProcedureCall getHibernateProcedureCall();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariant overrides

	@Override
	StoredProcedureQueryImplementor setHint(String hintName, Object value);

	@Override
	<T> StoredProcedureQueryImplementor setParameter(Parameter<T> param, T value);

	@Override
	StoredProcedureQueryImplementor setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override
	StoredProcedureQueryImplementor setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	StoredProcedureQueryImplementor setParameter(String name, Object value);

	@Override
	StoredProcedureQueryImplementor setParameter(String name, Calendar value, TemporalType temporalType);

	@Override
	StoredProcedureQueryImplementor setParameter(String name, Date value, TemporalType temporalType);

	@Override
	StoredProcedureQueryImplementor setParameter(int position, Object value);

	@Override
	StoredProcedureQueryImplementor setParameter(int position, Calendar value, TemporalType temporalType);

	@Override
	StoredProcedureQueryImplementor setParameter(int position, Date value, TemporalType temporalType);

	@Override
	StoredProcedureQueryImplementor setFlushMode(FlushModeType flushMode);

	@Override
	StoredProcedureQueryImplementor registerStoredProcedureParameter(int position, Class type, ParameterMode mode);

	@Override
	StoredProcedureQueryImplementor registerStoredProcedureParameter(String parameterName, Class type, ParameterMode mode);

	@Override
	StoredProcedureQueryImplementor setMaxResults(int maxResult);

	@Override
	StoredProcedureQueryImplementor setFirstResult(int startPosition);

	@Override
	StoredProcedureQueryImplementor setLockMode(LockModeType lockMode);
}
