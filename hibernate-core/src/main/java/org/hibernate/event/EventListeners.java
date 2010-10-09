/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.event;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.event.def.DefaultAutoFlushEventListener;
import org.hibernate.event.def.DefaultDeleteEventListener;
import org.hibernate.event.def.DefaultDirtyCheckEventListener;
import org.hibernate.event.def.DefaultEvictEventListener;
import org.hibernate.event.def.DefaultFlushEntityEventListener;
import org.hibernate.event.def.DefaultFlushEventListener;
import org.hibernate.event.def.DefaultInitializeCollectionEventListener;
import org.hibernate.event.def.DefaultLoadEventListener;
import org.hibernate.event.def.DefaultLockEventListener;
import org.hibernate.event.def.DefaultMergeEventListener;
import org.hibernate.event.def.DefaultPersistEventListener;
import org.hibernate.event.def.DefaultPostLoadEventListener;
import org.hibernate.event.def.DefaultPreLoadEventListener;
import org.hibernate.event.def.DefaultRefreshEventListener;
import org.hibernate.event.def.DefaultReplicateEventListener;
import org.hibernate.event.def.DefaultSaveEventListener;
import org.hibernate.event.def.DefaultSaveOrUpdateCopyEventListener;
import org.hibernate.event.def.DefaultSaveOrUpdateEventListener;
import org.hibernate.event.def.DefaultUpdateEventListener;
import org.hibernate.event.def.DefaultPersistOnFlushEventListener;
import org.hibernate.util.Cloneable;

/**
 * A convience holder for all defined session event listeners.
 *
 * @author Steve Ebersole
 */
public class EventListeners extends Cloneable implements Serializable {

	private LoadEventListener[] loadEventListeners = { new DefaultLoadEventListener() };
	private SaveOrUpdateEventListener[] saveOrUpdateEventListeners = { new DefaultSaveOrUpdateEventListener() };
	private MergeEventListener[] mergeEventListeners = { new DefaultMergeEventListener() };
	private PersistEventListener[] persistEventListeners = { new DefaultPersistEventListener() };
	private PersistEventListener[] persistOnFlushEventListeners = { new DefaultPersistOnFlushEventListener() };
	private ReplicateEventListener[] replicateEventListeners = { new DefaultReplicateEventListener() };
	private DeleteEventListener[] deleteEventListeners = { new DefaultDeleteEventListener() };
	private AutoFlushEventListener[] autoFlushEventListeners = { new DefaultAutoFlushEventListener() };
	private DirtyCheckEventListener[] dirtyCheckEventListeners = { new DefaultDirtyCheckEventListener() };
	private FlushEventListener[] flushEventListeners = { new DefaultFlushEventListener() };
	private EvictEventListener[] evictEventListeners = { new DefaultEvictEventListener() };
	private LockEventListener[] lockEventListeners = { new DefaultLockEventListener() };
	private RefreshEventListener[] refreshEventListeners = { new DefaultRefreshEventListener() };
	private FlushEntityEventListener[] flushEntityEventListeners = { new DefaultFlushEntityEventListener() };
	private InitializeCollectionEventListener[] initializeCollectionEventListeners = 
			{ new DefaultInitializeCollectionEventListener() };

	private PostLoadEventListener[] postLoadEventListeners = { new DefaultPostLoadEventListener() };
	private PreLoadEventListener[] preLoadEventListeners = { new DefaultPreLoadEventListener() };
	
	private PreDeleteEventListener[] preDeleteEventListeners = {};
	private PreUpdateEventListener[] preUpdateEventListeners = {};
	private PreInsertEventListener[] preInsertEventListeners = {};
	private PostDeleteEventListener[] postDeleteEventListeners = {};
	private PostUpdateEventListener[] postUpdateEventListeners = {};
	private PostInsertEventListener[] postInsertEventListeners = {};
	private PostDeleteEventListener[] postCommitDeleteEventListeners = {};
	private PostUpdateEventListener[] postCommitUpdateEventListeners = {};
	private PostInsertEventListener[] postCommitInsertEventListeners = {};

	private PreCollectionRecreateEventListener[] preCollectionRecreateEventListeners = {};
	private PostCollectionRecreateEventListener[] postCollectionRecreateEventListeners = {};
	private PreCollectionRemoveEventListener[] preCollectionRemoveEventListeners = {};
	private PostCollectionRemoveEventListener[] postCollectionRemoveEventListeners = {};
	private PreCollectionUpdateEventListener[] preCollectionUpdateEventListeners = {};
	private PostCollectionUpdateEventListener[] postCollectionUpdateEventListeners = {};	

	private SaveOrUpdateEventListener[] saveEventListeners = { new DefaultSaveEventListener() };
	private SaveOrUpdateEventListener[] updateEventListeners = { new DefaultUpdateEventListener() };
	private MergeEventListener[] saveOrUpdateCopyEventListeners = { new DefaultSaveOrUpdateCopyEventListener() };//saveOrUpdateCopy() is deprecated!

	private static Map eventInterfaceFromType;

	static {
		eventInterfaceFromType = new HashMap();

		eventInterfaceFromType.put("auto-flush", AutoFlushEventListener.class);
		eventInterfaceFromType.put("merge", MergeEventListener.class);
		eventInterfaceFromType.put("create", PersistEventListener.class);
		eventInterfaceFromType.put("create-onflush", PersistEventListener.class);
		eventInterfaceFromType.put("delete", DeleteEventListener.class);
		eventInterfaceFromType.put("dirty-check", DirtyCheckEventListener.class);
		eventInterfaceFromType.put("evict", EvictEventListener.class);
		eventInterfaceFromType.put("flush", FlushEventListener.class);
		eventInterfaceFromType.put("flush-entity", FlushEntityEventListener.class);
		eventInterfaceFromType.put("load", LoadEventListener.class);
		eventInterfaceFromType.put("load-collection", InitializeCollectionEventListener.class);
		eventInterfaceFromType.put("lock", LockEventListener.class);
		eventInterfaceFromType.put("refresh", RefreshEventListener.class);
		eventInterfaceFromType.put("replicate", ReplicateEventListener.class);
		eventInterfaceFromType.put("save-update", SaveOrUpdateEventListener.class);
		eventInterfaceFromType.put("save", SaveOrUpdateEventListener.class);
		eventInterfaceFromType.put("update", SaveOrUpdateEventListener.class);
		eventInterfaceFromType.put("pre-load", PreLoadEventListener.class);
		eventInterfaceFromType.put("pre-update", PreUpdateEventListener.class);
		eventInterfaceFromType.put("pre-delete", PreDeleteEventListener.class);
		eventInterfaceFromType.put("pre-insert", PreInsertEventListener.class);
		eventInterfaceFromType.put("pre-collection-recreate", PreCollectionRecreateEventListener.class);
		eventInterfaceFromType.put("pre-collection-remove", PreCollectionRemoveEventListener.class);
		eventInterfaceFromType.put("pre-collection-update", PreCollectionUpdateEventListener.class);
		eventInterfaceFromType.put("post-load", PostLoadEventListener.class);
		eventInterfaceFromType.put("post-update", PostUpdateEventListener.class);
		eventInterfaceFromType.put("post-delete", PostDeleteEventListener.class);
		eventInterfaceFromType.put("post-insert", PostInsertEventListener.class);
		eventInterfaceFromType.put("post-commit-update", PostUpdateEventListener.class);
		eventInterfaceFromType.put("post-commit-delete", PostDeleteEventListener.class);
		eventInterfaceFromType.put("post-commit-insert", PostInsertEventListener.class);
		eventInterfaceFromType.put("post-collection-recreate", PostCollectionRecreateEventListener.class);
		eventInterfaceFromType.put("post-collection-remove", PostCollectionRemoveEventListener.class);
		eventInterfaceFromType.put("post-collection-update", PostCollectionUpdateEventListener.class);
		eventInterfaceFromType = Collections.unmodifiableMap( eventInterfaceFromType );
	}

	public Class getListenerClassFor(String type) {
		Class clazz = (Class) eventInterfaceFromType.get(type);
		
		if (clazz == null) {
			throw new MappingException("Unrecognized listener type [" + type + "]");
		}

		return clazz;
	}

	private static interface ListenerProcesser {
		public void processListener(Object listener);
	}

	private void processListeners(ListenerProcesser processer) {
		Field[] fields = getClass().getDeclaredFields();
		for ( int i = 0; i < fields.length; i++ ) {
			final Object[] listeners;
			try {
				Object fieldValue = fields[i].get(this);
				if ( fieldValue instanceof Object[] ) {
					listeners = ( Object[] ) fieldValue;
				}
				else {
					continue;
				}
			}
			catch ( Throwable t ) {
				throw new HibernateException( "could not init listeners", t );
			}

			int length = listeners.length;
			for ( int index = 0 ; index < length ; index++ ) {
				processer.processListener( listeners[index ] );
			}
		}
	}

	/**
	 * Call {@link Initializable#initialize} on any listeners that implement the
	 * {@link Initializable} interface.
	 *
	 * @param cfg The configuration.
	 */
	public void initializeListeners(final Configuration cfg) {
		try {
			processListeners(
					new ListenerProcesser() {
						public void processListener(Object listener) {
							if ( listener instanceof Initializable ) {
								( ( Initializable ) listener ).initialize( cfg );
							}
						}
					}
			);
		}
		catch ( Exception e ) {
			throw new HibernateException("could not init listeners", e);
		}
	}

	/**
	 * Call {@link Destructible#cleanup} on any listeners that implement the
	 * {@link Destructible} interface.
	 */
	public void destroyListeners() {
		try {
			processListeners(
					new ListenerProcesser() {
						public void processListener(Object listener) {
							if ( listener instanceof Destructible ) {
								( ( Destructible ) listener ).cleanup();
							}
						}
					}
			);
		}
		catch ( Exception e ) {
			throw new HibernateException("could not destruct listeners", e);
		}
	}

	public LoadEventListener[] getLoadEventListeners() {
        return loadEventListeners;
    }

    public void setLoadEventListeners(LoadEventListener[] loadEventListener) {
        this.loadEventListeners = loadEventListener;
    }

	public ReplicateEventListener[] getReplicateEventListeners() {
		return replicateEventListeners;
	}

	public void setReplicateEventListeners(ReplicateEventListener[] replicateEventListener) {
		this.replicateEventListeners = replicateEventListener;
	}

	public DeleteEventListener[] getDeleteEventListeners() {
		return deleteEventListeners;
	}

	public void setDeleteEventListeners(DeleteEventListener[] deleteEventListener) {
		this.deleteEventListeners = deleteEventListener;
	}

	public AutoFlushEventListener[] getAutoFlushEventListeners() {
		return autoFlushEventListeners;
	}

	public void setAutoFlushEventListeners(AutoFlushEventListener[] autoFlushEventListener) {
		this.autoFlushEventListeners = autoFlushEventListener;
	}

	public DirtyCheckEventListener[] getDirtyCheckEventListeners() {
		return dirtyCheckEventListeners;
	}

	public void setDirtyCheckEventListeners(DirtyCheckEventListener[] dirtyCheckEventListener) {
		this.dirtyCheckEventListeners = dirtyCheckEventListener;
	}

	public FlushEventListener[] getFlushEventListeners() {
		return flushEventListeners;
	}

	public void setFlushEventListeners(FlushEventListener[] flushEventListener) {
		this.flushEventListeners = flushEventListener;
	}

	public EvictEventListener[] getEvictEventListeners() {
		return evictEventListeners;
	}

	public void setEvictEventListeners(EvictEventListener[] evictEventListener) {
		this.evictEventListeners = evictEventListener;
	}

	public LockEventListener[] getLockEventListeners() {
		return lockEventListeners;
	}

	public void setLockEventListeners(LockEventListener[] lockEventListener) {
		this.lockEventListeners = lockEventListener;
	}

	public RefreshEventListener[] getRefreshEventListeners() {
		return refreshEventListeners;
	}

	public void setRefreshEventListeners(RefreshEventListener[] refreshEventListener) {
		this.refreshEventListeners = refreshEventListener;
	}

	public InitializeCollectionEventListener[] getInitializeCollectionEventListeners() {
		return initializeCollectionEventListeners;
	}

	public void setInitializeCollectionEventListeners(InitializeCollectionEventListener[] initializeCollectionEventListener) {
		this.initializeCollectionEventListeners = initializeCollectionEventListener;
	}
	
	public FlushEntityEventListener[] getFlushEntityEventListeners() {
		return flushEntityEventListeners;
	}
	
	public void setFlushEntityEventListeners(FlushEntityEventListener[] flushEntityEventListener) {
		this.flushEntityEventListeners = flushEntityEventListener;
	}
	
	public SaveOrUpdateEventListener[] getSaveOrUpdateEventListeners() {
		return saveOrUpdateEventListeners;
	}
	
	public void setSaveOrUpdateEventListeners(SaveOrUpdateEventListener[] saveOrUpdateEventListener) {
		this.saveOrUpdateEventListeners = saveOrUpdateEventListener;
	}
	
	public MergeEventListener[] getMergeEventListeners() {
		return mergeEventListeners;
	}
	
	public void setMergeEventListeners(MergeEventListener[] mergeEventListener) {
		this.mergeEventListeners = mergeEventListener;
	}
	
	public PersistEventListener[] getPersistEventListeners() {
		return persistEventListeners;
	}
	
	public void setPersistEventListeners(PersistEventListener[] createEventListener) {
		this.persistEventListeners = createEventListener;
	}

	public PersistEventListener[] getPersistOnFlushEventListeners() {
		return persistOnFlushEventListeners;
	}

	public void setPersistOnFlushEventListeners(PersistEventListener[] createEventListener) {
		this.persistOnFlushEventListeners = createEventListener;
	}
	
	public MergeEventListener[] getSaveOrUpdateCopyEventListeners() {
		return saveOrUpdateCopyEventListeners;
	}
	
	public void setSaveOrUpdateCopyEventListeners(MergeEventListener[] saveOrUpdateCopyEventListener) {
		this.saveOrUpdateCopyEventListeners = saveOrUpdateCopyEventListener;
	}
	
	public SaveOrUpdateEventListener[] getSaveEventListeners() {
		return saveEventListeners;
	}
	
	public void setSaveEventListeners(SaveOrUpdateEventListener[] saveEventListener) {
		this.saveEventListeners = saveEventListener;
	}
	
	public SaveOrUpdateEventListener[] getUpdateEventListeners() {
		return updateEventListeners;
	}
	
	public void setUpdateEventListeners(SaveOrUpdateEventListener[] updateEventListener) {
		this.updateEventListeners = updateEventListener;
	}

	public PostLoadEventListener[] getPostLoadEventListeners() {
		return postLoadEventListeners;
	}

	public void setPostLoadEventListeners(PostLoadEventListener[] postLoadEventListener) {
		this.postLoadEventListeners = postLoadEventListener;
	}

	public PreLoadEventListener[] getPreLoadEventListeners() {
		return preLoadEventListeners;
	}

	public void setPreLoadEventListeners(PreLoadEventListener[] preLoadEventListener) {
		this.preLoadEventListeners = preLoadEventListener;
	}

	public PreCollectionRecreateEventListener[] getPreCollectionRecreateEventListeners() {
		return preCollectionRecreateEventListeners;
	}

	public void setPreCollectionRecreateEventListeners(PreCollectionRecreateEventListener[] preCollectionRecreateEventListener) {
		this.preCollectionRecreateEventListeners = preCollectionRecreateEventListener;
	}

	public PreCollectionRemoveEventListener[] getPreCollectionRemoveEventListeners() {
		return preCollectionRemoveEventListeners;
	}

	public void setPreCollectionRemoveEventListeners(PreCollectionRemoveEventListener[] preCollectionRemoveEventListener) {
		this.preCollectionRemoveEventListeners = preCollectionRemoveEventListener;
	}

	public PreCollectionUpdateEventListener[] getPreCollectionUpdateEventListeners() {
		return preCollectionUpdateEventListeners;
	}

	public void setPreCollectionUpdateEventListeners(PreCollectionUpdateEventListener[] preCollectionUpdateEventListeners) {
		this.preCollectionUpdateEventListeners = preCollectionUpdateEventListeners;
	}

	public PostDeleteEventListener[] getPostDeleteEventListeners() {
		return postDeleteEventListeners;
	}
	
	public PostInsertEventListener[] getPostInsertEventListeners() {
		return postInsertEventListeners;
	}
	
	public PostUpdateEventListener[] getPostUpdateEventListeners() {
		return postUpdateEventListeners;
	}
	
	public void setPostDeleteEventListeners(PostDeleteEventListener[] postDeleteEventListener) {
		this.postDeleteEventListeners = postDeleteEventListener;
	}
	
	public void setPostInsertEventListeners(PostInsertEventListener[] postInsertEventListener) {
		this.postInsertEventListeners = postInsertEventListener;
	}
	
	public void setPostUpdateEventListeners(PostUpdateEventListener[] postUpdateEventListener) {
		this.postUpdateEventListeners = postUpdateEventListener;
	}
	
	public PostCollectionRecreateEventListener[] getPostCollectionRecreateEventListeners() {
		return postCollectionRecreateEventListeners;
	}

	public void setPostCollectionRecreateEventListeners(PostCollectionRecreateEventListener[] postCollectionRecreateEventListener) {
		this.postCollectionRecreateEventListeners = postCollectionRecreateEventListener;
	}

	public PostCollectionRemoveEventListener[] getPostCollectionRemoveEventListeners() {
		return postCollectionRemoveEventListeners;
	}

	public void setPostCollectionRemoveEventListeners(PostCollectionRemoveEventListener[] postCollectionRemoveEventListener) {
		this.postCollectionRemoveEventListeners = postCollectionRemoveEventListener;
	}	        

	public PostCollectionUpdateEventListener[] getPostCollectionUpdateEventListeners() {
		return postCollectionUpdateEventListeners;
	}

	public void setPostCollectionUpdateEventListeners(PostCollectionUpdateEventListener[] postCollectionUpdateEventListeners) {
		this.postCollectionUpdateEventListeners = postCollectionUpdateEventListeners;
	}

	public PreDeleteEventListener[] getPreDeleteEventListeners() {
		return preDeleteEventListeners;
	}
	
	public void setPreDeleteEventListeners(PreDeleteEventListener[] preDeleteEventListener) {
		this.preDeleteEventListeners = preDeleteEventListener;
	}
	
	public PreInsertEventListener[] getPreInsertEventListeners() {
		return preInsertEventListeners;
	}
	
	public void setPreInsertEventListeners(PreInsertEventListener[] preInsertEventListener) {
		this.preInsertEventListeners = preInsertEventListener;
	}
	
	public PreUpdateEventListener[] getPreUpdateEventListeners() {
		return preUpdateEventListeners;
	}
	
	public void setPreUpdateEventListeners(PreUpdateEventListener[] preUpdateEventListener) {
		this.preUpdateEventListeners = preUpdateEventListener;
	}

	public PostDeleteEventListener[] getPostCommitDeleteEventListeners() {
		return postCommitDeleteEventListeners;
	}

	public void setPostCommitDeleteEventListeners(
			PostDeleteEventListener[] postCommitDeleteEventListeners) {
		this.postCommitDeleteEventListeners = postCommitDeleteEventListeners;
	}

	public PostInsertEventListener[] getPostCommitInsertEventListeners() {
		return postCommitInsertEventListeners;
	}

	public void setPostCommitInsertEventListeners(
			PostInsertEventListener[] postCommitInsertEventListeners) {
		this.postCommitInsertEventListeners = postCommitInsertEventListeners;
	}

	public PostUpdateEventListener[] getPostCommitUpdateEventListeners() {
		return postCommitUpdateEventListeners;
	}

	public void setPostCommitUpdateEventListeners(
			PostUpdateEventListener[] postCommitUpdateEventListeners) {
		this.postCommitUpdateEventListeners = postCommitUpdateEventListeners;
	}

}
