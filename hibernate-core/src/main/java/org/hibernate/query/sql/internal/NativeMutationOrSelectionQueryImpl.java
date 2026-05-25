/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import org.hibernate.ScrollableResults;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.query.MutationOrSelectionQuery;
import org.hibernate.query.Page;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.named.NamedNativeQueryMemento;
import org.hibernate.query.named.internal.NativeMutationMementoImpl;
import org.hibernate.query.named.internal.NativeSelectionMementoImpl;

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

	private final boolean mutation;

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

	@Override
	public boolean isSelectionQuery() {
		return !mutation;
	}

	@Override
	public boolean isMutationQuery() {
		return mutation;
	}

	private NativeMutationOrSelectionQueryImpl(
			NativeSelectionMementoImpl<?> selectionMemento,
			SharedSessionContractImplementor session) {
		super( selectionMemento, null, null, session );
		mutation = false;
	}

	private NativeMutationOrSelectionQueryImpl(
			NativeMutationMementoImpl<?> mutationMemento,
			SharedSessionContractImplementor session) {
		super( mutationMemento, session );
		mutation = true;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	public Object getSingleResult() {
		return super.getSingleResult();
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nullable
	public Object getSingleResultOrNull() {
		return super.getSingleResultOrNull();
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "unchecked", "removal"})
	@Nonnull
	public Stream getResultStream() {
		return super.getResultStream();
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "unchecked", "removal"})
	@Nonnull
	public List list() {
		return super.list();
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public ScrollableResults<Object> scroll() {
		return super.scroll();
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public ScrollableResults<Object> scroll(@Nonnull ScrollMode scrollMode) {
		return super.scroll( scrollMode );
	}

	@Override @Deprecated
	@SuppressWarnings({"rawtypes", "unchecked", "removal"})
	@Nonnull
	public Stream stream() {
		return super.stream();
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl addOption(@Nonnull Statement.Option option) {
		super.addOption( option );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setFlushMode(@Nonnull FlushModeType flushMode) {
		super.setFlushMode( flushMode );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setTimeout(@Nullable Integer timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setTimeout(@Nullable Timeout timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setComment(@Nullable String comment) {
		super.setComment( comment );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setHint(@Nonnull String hintName, @Nullable Object value) {
		super.setHint( hintName, value );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameter(@Nonnull String name, @Nullable P value, @Nonnull Class<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameter(@Nonnull String name, @Nullable P value, @Nonnull Type<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setParameter(@Nonnull String name, @Nullable Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setParameter(int position, @Nullable Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameter(int position, @Nullable P value, @Nonnull Class<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameter(int position, @Nullable P value, @Nonnull Type<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setConvertedParameter(
			@Nonnull String name,
			@Nullable P value,
			@Nonnull Class<? extends AttributeConverter<P, ?>> converter) {
		super.setConvertedParameter( name, value, converter );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setConvertedParameter(
			@Nonnull
			int position,
			@Nullable P value,
			@Nonnull Class<? extends AttributeConverter<P, ?>> converter) {
		super.setConvertedParameter( position, value, converter );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setParameter(@Nonnull String name, @Nullable Instant value, @Nonnull TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setParameter(@Nonnull String name, @Nullable Calendar value, @Nonnull TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setParameter(@Nonnull String name, @Nullable Date value, @Nonnull TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setParameter(int position, @Nullable Instant value, @Nonnull TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setParameter(int position, @Nullable Date value, @Nonnull TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setParameter(int position, @Nullable Calendar value, @Nonnull TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value, @Nonnull Class<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameter(@Nonnull QueryParameter<P> parameter, @Nullable P value, @Nonnull Type<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameter(@Nonnull Parameter<P> param, @Nullable P value) {
		super.setParameter( param, value );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setParameter(
			@Nonnull Parameter<Calendar> param,
			@Nullable Calendar value,
			@Nonnull TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override @Deprecated @SuppressWarnings("deprecation")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setParameter(@Nonnull Parameter<Date> param, @Nullable Date value, @Nonnull TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setParameterList(
			@Nonnull String name,
			@Nonnull @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			@Nonnull String name,
			@Nonnull Collection<? extends P> values,
			@Nonnull Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			@Nonnull String name,
			@Nonnull Collection<? extends P> values,
			@Nonnull Type<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setParameterList(@Nonnull String name, @Nonnull Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(@Nonnull String name, @Nonnull P[] values, @Nonnull Type<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setParameterList(
			@Nonnull
			int position,
			@Nonnull @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			@Nonnull
			int position,
			@Nonnull Collection<? extends P> values,
			@Nonnull Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			@Nonnull
			int position,
			@Nonnull Collection<? extends P> values,
			@Nonnull Type<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setParameterList(int position, @Nonnull Object[] values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(int position, @Nonnull P[] values, @Nonnull Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(int position, @Nonnull P[] values, @Nonnull Type<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			@Nonnull QueryParameter<P> parameter,
			@Nonnull Collection<? extends P> values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			@Nonnull QueryParameter<P> parameter,
			@Nonnull Collection<? extends P> values,
			@Nonnull Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			@Nonnull QueryParameter<P> parameter,
			@Nonnull Collection<? extends P> values,
			@Nonnull Type<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(
			@Nonnull QueryParameter<P> parameter,
			@Nonnull P[] values,
			@Nonnull Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	@Nonnull
	public <P> NativeMutationOrSelectionQueryImpl setParameterList(@Nonnull QueryParameter<P> parameter, @Nonnull P[] values, @Nonnull Type<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setProperties(@Nonnull Object bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setProperties(@Nonnull @SuppressWarnings("rawtypes") Map bean) {
		super.setProperties( bean );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setQueryFlushMode(@Nonnull QueryFlushMode queryFlushMode) {
		super.setQueryFlushMode( queryFlushMode );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl addOption(@Nonnull TypedQuery.Option option) {
		super.addOption( option );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("deprecation")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setEntityGraph(@Nonnull EntityGraph<? super Object> entityGraph) {
		super.setEntityGraph( entityGraph );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings({"deprecation", "removal"})
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setEntityGraph(@Nonnull EntityGraph<? super Object> graph, @Nonnull GraphSemantic semantic) {
		super.setEntityGraph( graph, semantic );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings({"deprecation", "removal"})
	@Nonnull
	public NativeMutationOrSelectionQueryImpl enableFetchProfile(@Nonnull String profileName) {
		super.enableFetchProfile( profileName );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings({"deprecation", "removal"})
	@Nonnull
	public NativeMutationOrSelectionQueryImpl disableFetchProfile(@Nonnull String profileName) {
		super.disableFetchProfile( profileName );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setFetchSize(int fetchSize) {
		super.setFetchSize( fetchSize );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setReadOnly(boolean readOnly) {
		super.setReadOnly( readOnly );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setMaxResults(int maxResults) {
		super.setMaxResults( maxResults );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
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
	@SuppressWarnings("removal")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setCacheMode(@Nonnull CacheMode cacheMode) {
		super.setCacheMode( cacheMode );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setCacheStoreMode(@Nonnull CacheStoreMode cacheStoreMode) {
		super.setCacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setCacheRetrieveMode(@Nonnull CacheRetrieveMode cacheRetrieveMode) {
		super.setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override @Deprecated
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setQueryPlanCacheable(boolean queryPlanCacheable) {
		super.setQueryPlanCacheable( queryPlanCacheable );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	public NativeMutationOrSelectionQueryImpl setCacheRegion(@Nullable String cacheRegion) {
		super.setCacheRegion( cacheRegion );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setLockMode(@Nonnull LockModeType lockMode) {
		super.setLockMode( lockMode );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setHibernateLockMode(@Nonnull LockMode lockMode) {
		super.setHibernateLockMode( lockMode );
		return this;
	}

	@Override @Deprecated
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setLockScope(@Nonnull PessimisticLockScope lockScope) {
		super.setLockScope( lockScope );
		return this;
	}

	@Override @Deprecated
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setLockTimeout(@Nullable Timeout lockTimeout) {
		super.setLockTimeout( lockTimeout );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setFollowOnStrategy(@Nonnull Locking.FollowOn followOnStrategy) {
		super.setFollowOnStrategy( followOnStrategy );
		return this;
	}

	@Override @Deprecated
	@SuppressWarnings("removal")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setCacheable(boolean cacheable) {
		super.setCacheable( cacheable );
		return this;
	}

	@Override
	@Nonnull
	public NativeMutationOrSelectionQueryImpl addQueryHint(@Nonnull String hint) {
		super.addQueryHint( hint );
		return this;
	}

	@Override
	@SuppressWarnings("removal")
	@Nonnull
	public NativeMutationOrSelectionQueryImpl setResultListTransformer(@Nonnull ResultListTransformer<Object> transformer) {
		super.setResultListTransformer( transformer );
		return this;
	}
}
