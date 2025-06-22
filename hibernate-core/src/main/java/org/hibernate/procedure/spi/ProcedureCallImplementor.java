/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.spi;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.named.NameableQuery;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.query.spi.QueryImplementor;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.TemporalType;
import jakarta.persistence.metamodel.Type;

/**
 * @author Steve Ebersole
 */
public interface ProcedureCallImplementor<R> extends ProcedureCall, NameableQuery, QueryImplementor<R> {
	@Override
	default List<R> getResultList() {
		return list();
	}

	ParameterStrategy getParameterStrategy();

	@Override
	FunctionReturnImplementor<R> getFunctionReturn();

	@Override
	ProcedureParameterMetadataImplementor getParameterMetadata();

	@Override
	R getSingleResult();

	@Override
	ProcedureCallImplementor<R> registerStoredProcedureParameter(int position, Class<?> type, ParameterMode mode);

	@Override
	ProcedureCallImplementor<R> registerStoredProcedureParameter(String parameterName, Class<?> type, ParameterMode mode);

	@Override
	ProcedureCallImplementor<R> registerStoredProcedureParameter(int position, Type<?> type, ParameterMode mode);

	@Override
	ProcedureCallImplementor<R> registerStoredProcedureParameter(String parameterName, Type<?> type, ParameterMode mode);

	@Override
	ProcedureCallImplementor<R> setHint(String hintName, Object value);

	@Override
	<T> ProcedureCallImplementor<R> setParameter(Parameter<T> param, T value);

	@Override @Deprecated
	ProcedureCallImplementor<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType);

	@Override @Deprecated
	ProcedureCallImplementor<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType);

	@Override
	ProcedureCallImplementor<R> setParameter(String name, Object value);

	@Override @Deprecated
	ProcedureCallImplementor<R> setParameter(String name, Calendar value, TemporalType temporalType);

	@Override @Deprecated
	ProcedureCallImplementor<R> setParameter(String name, Date value, TemporalType temporalType);

	@Override
	ProcedureCallImplementor<R> setParameter(int position, Object value);

	@Override @Deprecated
	ProcedureCallImplementor<R> setParameter(int position, Calendar value, TemporalType temporalType);

	@Override @Deprecated
	ProcedureCallImplementor<R> setParameter(int position, Date value, TemporalType temporalType);

	@Override
	ProcedureCallImplementor<R> setFlushMode(FlushModeType flushMode);

	@Override
	ProcedureCallImplementor<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode);

	@Override
	ProcedureCallImplementor<R> setCacheStoreMode(CacheStoreMode cacheStoreMode);

	@Override
	ProcedureCallImplementor<R> setTimeout(Integer timeout);

	@Override
	NamedCallableQueryMemento toMemento(String name);
}
