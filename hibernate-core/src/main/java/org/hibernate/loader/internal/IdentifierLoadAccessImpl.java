package org.hibernate.loader.internal;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.persistence.EntityGraph;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.CacheMode;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.UnknownProfileException;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.find.StatefulLoadAccessContext;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Standard implementation of load-by-id
 *
 * @author Steve Ebersole
 */
// Hibernate Reactive extends this class: see ReactiveIdentifierLoadAccessImpl
public class IdentifierLoadAccessImpl<T> implements IdentifierLoadAccess<T> {
	private final StatefulLoadAccessContext context;
	private final EntityPersister entityPersister;

	@Nullable
	private LockOptions lockOptions;
	@Nullable
	private CacheMode cacheMode;
	@Nullable
	private Boolean readOnly;
	@Nullable
	private RootGraphImplementor<T> rootGraph;
	@Nullable
	private GraphSemantic graphSemantic;
	@Nullable
	private Set<String> enabledFetchProfiles;
	@Nullable
	private Set<String> disabledFetchProfiles;

	public IdentifierLoadAccessImpl(@Nonnull StatefulLoadAccessContext context, @Nonnull EntityPersister entityPersister) {
		this.context = context;
		this.entityPersister = entityPersister;
	}

	@Nonnull
	@Override
	public final IdentifierLoadAccessImpl<T> with(@Nonnull LockOptions lockOptions) {
		this.lockOptions = lockOptions;
		return this;
	}

	@Nonnull
	@Override
	public IdentifierLoadAccess<T> with(@Nonnull LockMode lockMode, @Nonnull PessimisticLockScope lockScope) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setLockMode( lockMode );
		lockOptions.setLockScope( lockScope );
		return this;
	}

	@Nonnull
	@Override
	public IdentifierLoadAccess<T> with(@Nonnull Timeout timeout) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setTimeOut( timeout.milliseconds() );
		return this;
	}

	@Nonnull
	@Override
	public IdentifierLoadAccess<T> with(@Nonnull CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	@Nonnull
	@Override
	public IdentifierLoadAccess<T> withReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

	@Nonnull
	@Override
	public IdentifierLoadAccess<T> with(@Nonnull EntityGraph<T> graph, @Nonnull GraphSemantic semantic) {
		this.rootGraph = (RootGraphImplementor<T>) graph;
		this.graphSemantic = semantic;
		return this;
	}

	@Nonnull
	@Override
	public final T getReference(@Nonnull Object id) {
		return perform( () -> doGetReference( id ) );
	}

	// Hibernate Reactive overrides this
	@Nonnull
	protected T perform(@Nonnull Supplier<T> executor) {
		final var session = context.getSession();
		final var sessionCacheMode = session.getCacheMode();

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
			final var influencers = session.getLoadQueryInfluencers();
			final var fetchProfiles = influencers.adjustFetchProfiles( disabledFetchProfiles, enabledFetchProfiles );
			final var effectiveEntityGraph =
					rootGraph == null
							? null
							: influencers.applyEntityGraph( rootGraph, castNonNull( graphSemantic ));
			try {
				return executor.get();
			}
			finally {
				if ( effectiveEntityGraph != null ) {
					effectiveEntityGraph.clear();
				}
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

	// Hibernate Reactive overrides this
	@Nonnull
	protected T doGetReference(@Nonnull Object id) {
		final var session = context.getSession();
		final var concreteType = entityPersister.resolveConcreteProxyTypeForId( id, session );
		final Object result =
				context.load( LoadEventListener.LOAD, coerceId( id, session.getFactory() ),
						concreteType.getEntityName(), lockOptions, isReadOnly( session ) );
		//noinspection unchecked
		return (T) result;
	}

	// Hibernate Reactive might need to call this
	@Nullable
	protected Boolean isReadOnly(@Nonnull SessionImplementor session) {
		return readOnly != null
				? readOnly
				: session.getLoadQueryInfluencers().getReadOnly();
	}

	@Nonnull
	@Override
	public final T load(@Nonnull Object id) {
		return perform( () -> doLoad( id ) );
	}

	@Nonnull
	@Override
	public Optional<T> loadOptional(@Nonnull Object id) {
		return Optional.ofNullable( perform( () -> doLoad( id ) ) );
	}

	// Hibernate Reactive overrides this
	@Nullable
	protected T doLoad(@Nonnull Object id) {
		final var session = context.getSession();
		Object result;
		try {
			result =
					context.load( LoadEventListener.GET, coerceId( id, session.getFactory() ),
							entityPersister.getEntityName(), lockOptions, isReadOnly( session ) );
		}
		catch (ObjectNotFoundException notFoundException) {
			// if session cache contains proxy for nonexisting object
			result = null;
		}
		initializeIfNecessary( result );
		//noinspection unchecked
		return (T) result;
	}

	// Used by Hibernate Reactive
	@Nonnull
	protected Object coerceId(@Nonnull Object id, @Nonnull SessionFactoryImplementor factory) {
		if ( isLoadByIdComplianceEnabled( factory ) ) {
			return id;
		}
		else {
			try {
				final var identifierMapping = entityPersister.getIdentifierMapping();
				return identifierMapping.isVirtual()
						? id // special case for a class with an @IdClass
						: identifierMapping.getJavaType().coerce( id );
			}
			catch ( Exception e ) {
				throw new IllegalArgumentException( "Argument '" + id
						+ "' could not be converted to the identifier type of entity '"
						+ entityPersister.getEntityName() + "'"
						+ " [" + e.getMessage() + "]", e );
			}
		}
	}

	private void initializeIfNecessary(@Nullable Object result) {
		if ( result != null ) {
			final var lazyInitializer = extractLazyInitializer( result );
			if ( lazyInitializer != null ) {
				if ( lazyInitializer.isUninitialized() ) {
					lazyInitializer.initialize();
				}
			}
			else {
				final var enhancementMetadata = entityPersister.getBytecodeEnhancementMetadata();
				if ( enhancementMetadata.isEnhancedForLazyLoading()
						&& enhancementMetadata.extractLazyInterceptor( result )
								instanceof EnhancementAsProxyLazinessInterceptor lazinessInterceptor ) {
					lazinessInterceptor.forceInitialize( result, null );
				}
			}
		}
	}

	private static boolean isLoadByIdComplianceEnabled(@Nonnull SessionFactoryImplementor factory) {
		return factory.getSessionFactoryOptions().getJpaCompliance().isLoadByIdComplianceEnabled();
	}

	@Nonnull
	@Override
	public IdentifierLoadAccess<T> enableFetchProfile(@Nonnull String profileName) {
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

	@Nonnull
	@Override
	public IdentifierLoadAccess<T> disableFetchProfile(@Nonnull String profileName) {
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

	@Nullable
	protected CacheMode getCacheMode() {
		return cacheMode;
	}

	@Nullable
	protected GraphSemantic getGraphSemantic() {
		return graphSemantic;
	}

	@Nonnull
	protected StatefulLoadAccessContext getContext() {
		return context;
	}

	@Nonnull
	protected EntityPersister getEntityPersister() {
		return entityPersister;
	}

	@Nullable
	protected LockOptions getLockOptions() {
		return lockOptions;
	}

	@Nullable
	public RootGraphImplementor<T> getRootGraph() {
		return rootGraph;
	}
}
