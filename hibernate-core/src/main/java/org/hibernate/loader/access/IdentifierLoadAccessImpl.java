package org.hibernate.loader.access;

import java.util.Optional;
import java.util.function.Supplier;

import org.hibernate.CacheMode;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.LockOptions;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public class IdentifierLoadAccessImpl<T> implements IdentifierLoadAccess<T> {
	private final LoadAccessContext context;
	private final EntityPersister entityPersister;

	private LockOptions lockOptions;
	private CacheMode cacheMode;
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
		final EventSource eventSource = (EventSource) session;
		final LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();

		if ( this.lockOptions != null ) {
			LoadEvent event = new LoadEvent( id, entityPersister.getEntityName(), lockOptions, eventSource, loadQueryInfluencers.getReadOnly() );
			context.fireLoad( event, LoadEventListener.LOAD );
			return (T) event.getResult();
		}

		LoadEvent event = new LoadEvent( id, entityPersister.getEntityName(), false, eventSource, loadQueryInfluencers.getReadOnly() );
		boolean success = false;
		try {
			context.fireLoad( event, LoadEventListener.LOAD );
			if ( event.getResult() == null ) {
				session.getFactory().getEntityNotFoundDelegate().handleEntityNotFound(
						entityPersister.getEntityName(),
						id
				);
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
		final EventSource eventSource = (EventSource) session;
		final LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();

		if ( this.lockOptions != null ) {
			LoadEvent event = new LoadEvent( id, entityPersister.getEntityName(), lockOptions, eventSource, loadQueryInfluencers.getReadOnly() );
			context.fireLoad( event, LoadEventListener.GET );
			return (T) event.getResult();
		}

		LoadEvent event = new LoadEvent( id, entityPersister.getEntityName(), false, eventSource, loadQueryInfluencers.getReadOnly() );
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
		return (T) event.getResult();
	}
}
