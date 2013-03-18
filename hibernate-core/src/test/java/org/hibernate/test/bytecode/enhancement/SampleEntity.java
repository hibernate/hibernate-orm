/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.bytecode.enhancement;

import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;

/**
 * @author Steve Ebersole
 */
public class SampleEntity implements ManagedEntity, PersistentAttributeInterceptable {
	@Transient
	private transient EntityEntry entityEntry;
	@Transient
	private transient ManagedEntity previous;
	@Transient
	private transient ManagedEntity next;
	@Transient
	private transient PersistentAttributeInterceptor interceptor;

	private Long id;
	private String name;

	@Id
	public Long getId() {
		return hibernate_read_id();
	}

	public void setId(Long id) {
		hibernate_write_id( id );
	}

	public String getName() {
		return hibernate_read_name();
	}

	public void setName(String name) {
		hibernate_write_name( name );
	}

	private Long hibernate_read_id() {
		if ( $$_hibernate_getInterceptor() != null ) {
			this.id = (Long) $$_hibernate_getInterceptor().readObject( this, "id", this.id );
		}
		return id;
	}

	private void hibernate_write_id(Long id) {
		Long localVar = id;
		if ( $$_hibernate_getInterceptor() != null ) {
			localVar = (Long) $$_hibernate_getInterceptor().writeObject( this, "id", this.id, id );
		}
		this.id = localVar;
	}

	private String hibernate_read_name() {
		if ( $$_hibernate_getInterceptor() != null ) {
			this.name = (String) $$_hibernate_getInterceptor().readObject( this, "name", this.name );
		}
		return name;
	}

	private void hibernate_write_name(String name) {
		String localName = name;
		if ( $$_hibernate_getInterceptor() != null ) {
			localName = (String) $$_hibernate_getInterceptor().writeObject( this, "name", this.name, name );
		}
		this.name = localName;
	}

	@Override
	public Object $$_hibernate_getEntityInstance() {
		return this;
	}

	@Override
	public EntityEntry $$_hibernate_getEntityEntry() {
		return entityEntry;
	}

	@Override
	public void $$_hibernate_setEntityEntry(EntityEntry entityEntry) {
		this.entityEntry = entityEntry;
	}

	@Override
	public ManagedEntity $$_hibernate_getNextManagedEntity() {
		return next;
	}

	@Override
	public void $$_hibernate_setNextManagedEntity(ManagedEntity next) {
		this.next = next;
	}

	@Override
	public ManagedEntity $$_hibernate_getPreviousManagedEntity() {
		return previous;
	}

	@Override
	public void $$_hibernate_setPreviousManagedEntity(ManagedEntity previous) {
		this.previous = previous;
	}

	@Override
	public PersistentAttributeInterceptor $$_hibernate_getInterceptor() {
		return interceptor;
	}

	@Override
	public void $$_hibernate_setInterceptor(PersistentAttributeInterceptor interceptor) {
		this.interceptor = interceptor;
	}
}
