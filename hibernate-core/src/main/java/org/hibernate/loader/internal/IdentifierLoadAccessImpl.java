/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.loader.internal;

import java.util.Optional;
import java.util.function.Supplier;

import org.hibernate.CacheMode;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.LockOptions;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Standard implementation of load-by-id
 *
 * @author Steve Ebersole
 */
public class IdentifierLoadAccessImpl<T> implements IdentifierLoadAccess<T>, JavaType.CoercionContext {
	private final LoadAccessContext context;
	private final EntityPersister entityPersister;

	private LockOptions lockOptions;
	private CacheMode cacheMode;
	private Boolean readOnly;
	private RootGraphImplementor<T> rootGraph;
	private GraphSemantic graphSemantic;

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

	protected T perform(Supplier<T> executor) {
		final SessionImplementor session = context.getSession();

		CacheMode sessionCacheMode = session.getCacheMode();
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
			if ( graphSemantic != null ) {
				if ( rootGraph == null ) {
					throw new IllegalArgumentException( "Graph semantic specified, but no RootGraph was supplied" );
				}
				session.getLoadQueryInfluencers().getEffectiveEntityGraph().applyGraph( rootGraph, graphSemantic );
			}

			try {
				return executor.get();
			}
			finally {
				if ( graphSemantic != null ) {
					session.getLoadQueryInfluencers().getEffectiveEntityGraph().clear();
				}
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
	protected T doGetReference(Object id) {
		final SessionImplementor session = context.getSession();
		final EventSource eventSource = session.asEventSource();
		final LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();

		final JpaCompliance jpaCompliance = session.getFactory().getSessionFactoryOptions().getJpaCompliance();
		if ( ! jpaCompliance.isLoadByIdComplianceEnabled() ) {
			id = entityPersister.getIdentifierMapping().getJavaType().coerce( id, this );
		}

		String entityName = entityPersister.getEntityName();
		Boolean readOnly = this.readOnly != null ? this.readOnly : loadQueryInfluencers.getReadOnly();

		if ( lockOptions != null ) {
			LoadEvent event = new LoadEvent( id, entityName, lockOptions, eventSource, readOnly );
			context.fireLoad( event, LoadEventListener.LOAD );
			return (T) event.getResult();
		}

		LoadEvent event = new LoadEvent( id, entityName, false, eventSource, readOnly );
		boolean success = false;
		try {
			context.fireLoad( event, LoadEventListener.LOAD );
			if ( event.getResult() == null ) {
				session.getFactory().getEntityNotFoundDelegate().handleEntityNotFound( entityName, id );
			}
			success = true;
			return (T) event.getResult();
		}
		finally {
			context.afterOperation( success );
		}
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
	protected final T doLoad(Object id) {
		final SessionImplementor session = context.getSession();
		final EventSource eventSource = session.asEventSource();
		final LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();

		final JpaCompliance jpaCompliance = session.getFactory().getSessionFactoryOptions().getJpaCompliance();
		if ( ! jpaCompliance.isLoadByIdComplianceEnabled() ) {
			try {
				id = entityPersister.getIdentifierMapping().getJavaType().coerce( id, this );
			}
			catch ( Exception e ) {
				throw new IllegalArgumentException( "Argument '" + id
						+ "' could not be converted to the identifier type of entity '" + entityPersister.getEntityName() + "'"
						+ " [" + e.getMessage() + "]", e );
			}
		}

		String entityName = entityPersister.getEntityName();
		Boolean readOnly = this.readOnly != null ? this.readOnly : loadQueryInfluencers.getReadOnly();

		if ( lockOptions != null ) {
			LoadEvent event = new LoadEvent( id, entityName, lockOptions, eventSource, readOnly );
			context.fireLoad( event, LoadEventListener.GET );
			final Object result = event.getResult();
			initializeIfNecessary( result );

			return (T) result;
		}

		LoadEvent event = new LoadEvent( id, entityName, false, eventSource, readOnly );
		boolean success = false;
		try {
			context.fireLoad( event, LoadEventListener.GET );
			success = true;
		}
		catch (ObjectNotFoundException e) {
			// if session cache contains proxy for non-existing object
		}
		finally {
			context.afterOperation( success );
		}

		final Object result = event.getResult();
		initializeIfNecessary( result );

		return (T) result;
	}

	private void initializeIfNecessary(Object result) {
		if ( result == null ) {
			return;
		}

		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( result );
		if ( lazyInitializer != null ) {
			if ( lazyInitializer.isUninitialized() ) {
				lazyInitializer.initialize();
			}
		}
		else {
			final BytecodeEnhancementMetadata enhancementMetadata = entityPersister.getEntityMetamodel().getBytecodeEnhancementMetadata();
			if ( enhancementMetadata.isEnhancedForLazyLoading() ) {
				final BytecodeLazyAttributeInterceptor interceptor = enhancementMetadata.extractLazyInterceptor( result);
				if ( interceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
					( (EnhancementAsProxyLazinessInterceptor) interceptor ).forceInitialize( result, null );
				}
			}
		}
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return context.getSession().getSessionFactory().getTypeConfiguration();
	}
}
