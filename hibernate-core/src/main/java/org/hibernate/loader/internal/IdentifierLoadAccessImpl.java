/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.internal;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.CacheMode;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.LockOptions;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.UnknownProfileException;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Standard implementation of load-by-id
 *
 * @author Steve Ebersole
 */
// Hibernate Reactive extends this class: see ReactiveIdentifierLoadAccessImpl
public class IdentifierLoadAccessImpl<T> implements IdentifierLoadAccess<T>, JavaType.CoercionContext {
	private final LoadAccessContext context;
	private final EntityPersister entityPersister;

	private LockOptions lockOptions;
	private CacheMode cacheMode;
	private Boolean readOnly;
	private RootGraphImplementor<T> rootGraph;
	private GraphSemantic graphSemantic;
	private Set<String> enabledFetchProfiles;
	private Set<String> disabledFetchProfiles;

	public IdentifierLoadAccessImpl(LoadAccessContext context, EntityPersister entityPersister) {
		this.context = context;
		this.entityPersister = entityPersister;
	}

	@Override
	public final IdentifierLoadAccessImpl<T> with(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
		return this;
	}

	@Override
	public IdentifierLoadAccess<T> with(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	@Override
	public IdentifierLoadAccess<T> withReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

	@Override
	public IdentifierLoadAccess<T> with(RootGraph<T> graph, GraphSemantic semantic) {
		this.rootGraph = (RootGraphImplementor<T>) graph;
		this.graphSemantic = semantic;
		return this;
	}

	@Override
	public final T getReference(Object id) {
		return perform( () -> doGetReference( id ) );
	}

	// Hibernate Reactive overrides this
	protected T perform(Supplier<T> executor) {
		final SessionImplementor session = context.getSession();
		final CacheMode sessionCacheMode = session.getCacheMode();

		boolean cacheModeChanged = false;
		if ( cacheMode != null ) {
			// naive check for now...
			// todo : account for "conceptually equal"
			if ( cacheMode != sessionCacheMode ) {
				session.setCacheMode( cacheMode );
				cacheModeChanged = true;
			}
		}

		try {
			final LoadQueryInfluencers influencers = session.getLoadQueryInfluencers();
			final HashSet<String> fetchProfiles =
					influencers.adjustFetchProfiles( disabledFetchProfiles, enabledFetchProfiles );
			final EffectiveEntityGraph effectiveEntityGraph =
					influencers.applyEntityGraph( rootGraph, graphSemantic);
			try {
				return executor.get();
			}
			finally {
				effectiveEntityGraph.clear();
				influencers.setEnabledFetchProfileNames( fetchProfiles );
			}
		}
		finally {
			if ( cacheModeChanged ) {
				// change it back
				session.setCacheMode( sessionCacheMode );
			}
		}
	}

	@SuppressWarnings( "unchecked" )
	// Hibernate Reactive overrides this
	protected T doGetReference(Object id) {
		final SessionImplementor session = context.getSession();
		final EntityMappingType concreteType = entityPersister.resolveConcreteProxyTypeForId( id, session );
		return (T) context.load( LoadEventListener.LOAD, coerceId( id, session.getFactory() ),
				concreteType.getEntityName(), lockOptions, isReadOnly( session ) );
	}

	// Hibernate Reactive might need to call this
	protected Boolean isReadOnly(SessionImplementor session) {
		return readOnly != null
				? readOnly
				: session.getLoadQueryInfluencers().getReadOnly();
	}

	@Override
	public final T load(Object id) {
		return perform( () -> doLoad( id ) );
	}

	@Override
	public Optional<T> loadOptional(Object id) {
		return Optional.ofNullable( perform( () -> doLoad( id ) ) );
	}

	@SuppressWarnings( "unchecked" )
	// Hibernate Reactive overrides this
	protected T doLoad(Object id) {
		final SessionImplementor session = context.getSession();
		Object result;
		try {
			result = context.load( LoadEventListener.GET, coerceId( id, session.getFactory() ),
					entityPersister.getEntityName(), lockOptions, isReadOnly( session ) );
		}
		catch (ObjectNotFoundException notFoundException) {
			// if session cache contains proxy for non-existing object
			result = null;
		}
		initializeIfNecessary( result );
		return (T) result;
	}

	// Used by Hibernate Reactive
	protected Object coerceId(Object id, SessionFactoryImplementor factory) {
		if ( isLoadByIdComplianceEnabled( factory ) ) {
			return id;
		}
		else {
			try {
				return entityPersister.getIdentifierMapping().getJavaType().coerce( id, this );
			}
			catch ( Exception e ) {
				throw new IllegalArgumentException( "Argument '" + id
						+ "' could not be converted to the identifier type of entity '"
						+ entityPersister.getEntityName() + "'"
						+ " [" + e.getMessage() + "]", e );
			}
		}
	}

	private void initializeIfNecessary(Object result) {
		if ( result != null ) {
			final LazyInitializer lazyInitializer = extractLazyInitializer( result );
			if ( lazyInitializer != null ) {
				if ( lazyInitializer.isUninitialized() ) {
					lazyInitializer.initialize();
				}
			}
			else {
				final BytecodeEnhancementMetadata enhancementMetadata =
						entityPersister.getEntityMetamodel().getBytecodeEnhancementMetadata();
				if ( enhancementMetadata.isEnhancedForLazyLoading() ) {
					final BytecodeLazyAttributeInterceptor interceptor =
							enhancementMetadata.extractLazyInterceptor( result);
					if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor lazinessInterceptor ) {
						lazinessInterceptor.forceInitialize( result, null );
					}
				}
			}
		}
	}

	private static boolean isLoadByIdComplianceEnabled(SessionFactoryImplementor factory) {
		return factory.getSessionFactoryOptions().getJpaCompliance().isLoadByIdComplianceEnabled();
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return context.getSession().getSessionFactory().getTypeConfiguration();
	}

	@Override
	public IdentifierLoadAccess<T> enableFetchProfile(String profileName) {
		if ( !context.getSession().getFactory().containsFetchProfileDefinition( profileName ) ) {
			throw new UnknownProfileException( profileName );
		}
		if ( enabledFetchProfiles == null ) {
			enabledFetchProfiles = new HashSet<>();
		}
		enabledFetchProfiles.add( profileName );
		if ( disabledFetchProfiles != null ) {
			disabledFetchProfiles.remove( profileName );
		}
		return this;
	}

	@Override
	public IdentifierLoadAccess<T> disableFetchProfile(String profileName) {
		if ( disabledFetchProfiles == null ) {
			disabledFetchProfiles = new HashSet<>();
		}
		disabledFetchProfiles.add( profileName );
		if ( enabledFetchProfiles != null ) {
			enabledFetchProfiles.remove( profileName );
		}
		return this;
	}

	// Getters for Hibernate Reactive

	protected CacheMode getCacheMode() {
		return cacheMode;
	}

	protected GraphSemantic getGraphSemantic() {
		return graphSemantic;
	}

	protected LoadAccessContext getContext() {
		return context;
	}

	protected EntityPersister getEntityPersister() {
		return entityPersister;
	}

	protected LockOptions getLockOptions() {
		return lockOptions;
	}

	public RootGraphImplementor<T> getRootGraph() {
		return rootGraph;
	}
}
