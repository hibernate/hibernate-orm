/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * Occurs after an entity instance is fully loaded.
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>, Gavin King
 */
public class PostLoadEvent extends AbstractEvent {
	private Object entity;
	private Object id;
	private EntityTypeDescriptor descriptor;

	public PostLoadEvent(EventSource session) {
		super(session);
	}

	public void reset() {
		entity = null;
		id = null;
		descriptor = null;
	}

	public Object getEntity() {
		return entity;
	}

	/**
	 *
	 * @deprecated use {@link #getDescriptor()}
	 */
	@Deprecated
	public EntityTypeDescriptor getPersister() {
		return descriptor;
	}

	public EntityTypeDescriptor getDescriptor() {
		return descriptor;
	}
	
	public Object getId() {
		return id;
	}

	public PostLoadEvent setEntity(Object entity) {
		this.entity = entity;
		return this;
	}
	
	public PostLoadEvent setId(Object id) {
		this.id = id;
		return this;
	}

	/**
	 *
	 * @deprecated use {@link #setDescriptor(EntityTypeDescriptor)}
	 */
	@Deprecated
	public PostLoadEvent setPersister(EntityTypeDescriptor persister) {
		this.descriptor = persister;
		return this;
	}

	public PostLoadEvent setDescriptor(EntityTypeDescriptor descriptor){
		this.descriptor = descriptor;
		return this;
	}
}
