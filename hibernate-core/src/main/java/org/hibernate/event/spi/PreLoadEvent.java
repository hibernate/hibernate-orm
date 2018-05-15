/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.secure.spi.PermissionCheckEntityInformation;

/**
 * Called before injecting property values into a newly loaded entity instance.
 *
 * @author Gavin King
 */
public class PreLoadEvent extends AbstractEvent implements PermissionCheckEntityInformation {
	private Object entity;
	private Object[] state;
	private Object id;
	private EntityTypeDescriptor descriptor;

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
	
	public Object getId() {
		return id;
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
	
	public Object[] getState() {
		return state;
	}

	public PreLoadEvent setEntity(Object entity) {
		this.entity = entity;
		return this;
	}
	
	public PreLoadEvent setId(Object id) {
		this.id = id;
		return this;
	}

	/**
	 *
	 * @deprecated use {@link #setDescriptor(EntityTypeDescriptor)}
	 */
	@Deprecated
	public PreLoadEvent setPersister(EntityTypeDescriptor descriptor) {
		this.descriptor = descriptor;
		return this;
	}

	public PreLoadEvent setDescriptor(EntityTypeDescriptor descriptor) {
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
	public Object getIdentifier() {
		return id;
	}
}
