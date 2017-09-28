/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.io.Serializable;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;

/**
 * Occurs afterQuery an an entity instance is fully loaded.
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>, Gavin King
 */
public class PostLoadEvent extends AbstractEvent {
	private Object entity;
	private Serializable id;
	private EntityDescriptor descriptor;

	public PostLoadEvent(EventSource session) {
		super(session);
	}

	public Object getEntity() {
		return entity;
	}

	/**
	 *
	 * @deprecated use {@link #getDescriptor()}
	 */
	@Deprecated
	public EntityDescriptor getPersister() {
		return descriptor;
	}

	public EntityDescriptor getDescriptor() {
		return descriptor;
	}
	
	public Serializable getId() {
		return id;
	}

	public PostLoadEvent setEntity(Object entity) {
		this.entity = entity;
		return this;
	}
	
	public PostLoadEvent setId(Serializable id) {
		this.id = id;
		return this;
	}

	/**
	 *
	 * @deprecated use {@link #setDescriptor(EntityDescriptor)}
	 */
	@Deprecated
	public PostLoadEvent setPersister(EntityDescriptor persister) {
		this.descriptor = persister;
		return this;
	}

	public PostLoadEvent setDescriptor(EntityDescriptor descriptor){
		this.descriptor = descriptor;
		return this;
	}
	
}
