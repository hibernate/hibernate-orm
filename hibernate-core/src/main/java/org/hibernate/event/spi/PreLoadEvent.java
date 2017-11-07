/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.io.Serializable;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.secure.spi.PermissionCheckEntityInformation;

/**
 * Called beforeQuery injecting property values into a newly loaded entity instance.
 *
 * @author Gavin King
 */
public class PreLoadEvent extends AbstractEvent implements PermissionCheckEntityInformation {
	private Object entity;
	private Object[] state;
	private Serializable id;
	private EntityDescriptor descriptor;

	public PreLoadEvent(EventSource session) {
		super( session );
	}

	public void reset() {
		entity = null;
		state = null;
		id = null;
		descriptor = null;
	}

	@Override
	public Object getEntity() {
		return entity;
	}
	
	public Serializable getId() {
		return id;
	}

	/**
	 *
	 * @deprecated use {@link #getDescriptor()}
	 */
	public EntityDescriptor getPersister() {
		return descriptor;
	}

	public EntityDescriptor getDescriptor() {
		return descriptor;
	}
	
	public Object[] getState() {
		return state;
	}

	public PreLoadEvent setEntity(Object entity) {
		this.entity = entity;
		return this;
	}
	
	public PreLoadEvent setId(Serializable id) {
		this.id = id;
		return this;
	}

	/**
	 *
	 * @deprecated use {@link #setDescriptor(EntityDescriptor)}
	 */
	@Deprecated
	public PreLoadEvent setPersister(EntityDescriptor descriptor) {
		this.descriptor = descriptor;
		return this;
	}

	public PreLoadEvent setDescriptor(EntityDescriptor descriptor) {
		this.descriptor = descriptor;
		return this;
	}
	
	public PreLoadEvent setState(Object[] state) {
		this.state = state;
		return this;
	}

	@Override
	public String getEntityName() {
		return descriptor.getEntityName();
	}

	@Override
	public Serializable getIdentifier() {
		return id;
	}
}
