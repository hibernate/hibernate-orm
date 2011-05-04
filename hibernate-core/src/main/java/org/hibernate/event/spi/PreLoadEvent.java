/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.event.spi;

import java.io.Serializable;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Called before injecting property values into a newly 
 * loaded entity instance.
 *
 * @author Gavin King
 */
public class PreLoadEvent extends AbstractEvent {
	private Object entity;
	private Object[] state;
	private Serializable id;
	private EntityPersister persister;

	public PreLoadEvent(EventSource session) {
		super(session);
	}

	public Object getEntity() {
		return entity;
	}
	
	public Serializable getId() {
		return id;
	}
	
	public EntityPersister getPersister() {
		return persister;
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
	
	public PreLoadEvent setPersister(EntityPersister persister) {
		this.persister = persister;
		return this;
	}
	
	public PreLoadEvent setState(Object[] state) {
		this.state = state;
		return this;
	}
	
}
