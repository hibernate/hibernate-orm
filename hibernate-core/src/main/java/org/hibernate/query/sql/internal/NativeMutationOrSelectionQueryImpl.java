/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryFlushMode;
import jakarta.persistence.Statement;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.metamodel.Type;
import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.query.MutationOrSelectionQuery;
import org.hibernate.query.Page;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.named.NamedNativeQueryMemento;
import org.hibernate.query.named.internal.NativeMutationMementoImpl;
import org.hibernate.query.named.internal.NativeSelectionMementoImpl;
import org.hibernate.query.spi.ScrollableResultsImplementor;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Native query implementation used for the legacy untyped named-query API.
 */
public class NativeMutationOrSelectionQueryImpl
		extends NativeQueryImpl<Object>
		implements MutationOrSelectionQuery {

	public static MutationOrSelectionQuery from(NamedNativeQueryMemento<?> memento, SharedSessionContractImplementor session) {
		if ( memento instanceof NativeSelectionMementoImpl<?> selectionMemento ) {
			return new NativeMutationOrSelectionQueryImpl( selectionMemento, session );
		}
		else if ( memento instanceof NativeMutationMementoImpl<?> mutationMemento ) {
			return new NativeMutationOrSelectionQueryImpl( mutationMemento, session );
		}
		else {
			throw new IllegalArgumentException( "Unknown memento type: " + memento.getClass() );
		}
	}

	private NativeMutationOrSelectionQueryImpl(
			NativeSelectionMementoImpl<?> selectionMemento,
			SharedSessionContractImplementor session) {
		super( selectionMemento, null, null, session );
	}

	private NativeMutationOrSelectionQueryImpl(
			NativeMutationMementoImpl<?> mutationMemento,
			SharedSessionContractImplementor session) {
		super( mutationMemento, session );
	}

	@Override @Deprecated
	public Object getSingleResult() {
		return super.getSingleResult();
	}

	@Override @Deprecated
	public Object getSingleResultOrNull() {
		return super.getSingleResultOrNull();
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Stream getResultStream() {
		return super.getResultStream();
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "unchecked"})
	public List list() {
		return super.list();
	}

	@Override @Deprecated
	public ScrollableResultsImplementor<Object> scroll() {
		return super.scroll();
	}

	@Override @Deprecated
	public ScrollableResultsImplementor<Object> scroll(ScrollMode scrollMode) {
		return super.scroll( scrollMode );
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Stream stream() {
		return super.stream();
	}

	@Override
	public NativeMutationOrSelectionQueryImpl addOption(Statement.Option option) {
		super.addOption( option );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl setFlushMode(FlushModeType flushMode) {
		super.setFlushMode( flushMode );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl setTimeout(Integer timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl setTimeout(Timeout timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl setComment(String comment) {
		super.setComment( comment );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl setHint(String hintName, Object value) {
		super.setHint( hintName, value );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameter(String name, P value, Class<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameter(String name, P value, Type<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameter(int position, P value, Class<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameter(int position, P value, Type<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setConvertedParameter(
			String name,
			P value,
			Class<? extends AttributeConverter<P, ?>> converter) {
		super.setConvertedParameter( name, value, converter );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setConvertedParameter(
			int position,
			P value,
			Class<? extends AttributeConverter<P, ?>> converter) {
		super.setConvertedParameter( position, value, converter );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public NativeMutationOrSelectionQueryImpl setParameter(String name, Instant value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public NativeMutationOrSelectionQueryImpl setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public NativeMutationOrSelectionQueryImpl setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public NativeMutationOrSelectionQueryImpl setParameter(int position, Instant value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public NativeMutationOrSelectionQueryImpl setParameter(int position, Date value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public NativeMutationOrSelectionQueryImpl setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameter(QueryParameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameter(QueryParameter<P> parameter, P value, Class<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameter(QueryParameter<P> parameter, P value, Type<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameter(Parameter<P> param, P value) {
		super.setParameter( param, value );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public NativeMutationOrSelectionQueryImpl setParameter(
			Parameter<Calendar> param,
			Calendar value,
			TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	public NativeMutationOrSelectionQueryImpl setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl setParameterList(
			String name,
			@SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			String name,
			Collection<? extends P> values,
			Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			String name,
			Collection<? extends P> values,
			Type<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl setParameterList(String name, Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(String name, P[] values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(String name, P[] values, Type<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl setParameterList(
			int position,
			@SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			int position,
			Collection<? extends P> values,
			Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			int position,
			Collection<? extends P> values,
			Type<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl setParameterList(int position, Object[] values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(int position, P[] values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(int position, P[] values, Type<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			QueryParameter<P> parameter,
			Collection<? extends P> values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			QueryParameter<P> parameter,
			Collection<? extends P> values,
			Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			QueryParameter<P> parameter,
			Collection<? extends P> values,
			Type<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(QueryParameter<P> parameter, P[] values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			QueryParameter<P> parameter,
			P[] values,
			Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(QueryParameter<P> parameter, P[] values, Type<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl setProperties(Object bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl setProperties(@SuppressWarnings("rawtypes") Map bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl setQueryFlushMode(QueryFlushMode queryFlushMode) {
		super.setQueryFlushMode( queryFlushMode );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl addOption(TypedQuery.Option option) {
		super.addOption( option );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setEntityGraph(EntityGraph<? super Object> entityGraph) {
		super.setEntityGraph( entityGraph );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setEntityGraph(EntityGraph<? super Object> graph, GraphSemantic semantic) {
		super.setEntityGraph( graph, semantic );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl enableFetchProfile(String profileName) {
		super.enableFetchProfile( profileName );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl disableFetchProfile(String profileName) {
		super.disableFetchProfile( profileName );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setFetchSize(int fetchSize) {
		super.setFetchSize( fetchSize );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setReadOnly(boolean readOnly) {
		super.setReadOnly( readOnly );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setMaxResults(int maxResults) {
		super.setMaxResults( maxResults );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setFirstResult(int startPosition) {
		super.setFirstResult( startPosition );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setPage(Page page) {
		super.setPage( page );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setCacheMode(CacheMode cacheMode) {
		super.setCacheMode( cacheMode );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		super.setCacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		super.setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setQueryPlanCacheable(boolean queryPlanCacheable) {
		super.setQueryPlanCacheable( queryPlanCacheable );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setCacheRegion(String cacheRegion) {
		super.setCacheRegion( cacheRegion );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setLockMode(LockModeType lockMode) {
		super.setLockMode( lockMode );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setHibernateLockMode(LockMode lockMode) {
		super.setHibernateLockMode( lockMode );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setLockScope(PessimisticLockScope lockScope) {
		super.setLockScope( lockScope );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setLockTimeout(Timeout lockTimeout) {
		super.setLockTimeout( lockTimeout );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setFollowOnStrategy(Locking.FollowOn followOnStrategy) {
		super.setFollowOnStrategy( followOnStrategy );
		return this;
	}

	@Override @Deprecated
	public NativeMutationOrSelectionQueryImpl setCacheable(boolean cacheable) {
		super.setCacheable( cacheable );
		return this;
	}

	@Override
	public NativeMutationOrSelectionQueryImpl addQueryHint(String hint) {
		super.addQueryHint( hint );
		return this;
	}
}
