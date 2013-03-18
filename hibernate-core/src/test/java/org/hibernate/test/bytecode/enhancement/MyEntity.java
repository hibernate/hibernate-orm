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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.EntityEntry;

/**
 * @author Steve Ebersole
 */
@Entity
public class MyEntity implements ManagedEntity {
	@Transient
	private transient EntityEntry entityEntry;
	@Transient
	private transient ManagedEntity previous;
	@Transient
	private transient ManagedEntity next;

	private Long id;
	private String name;

	public MyEntity() {
	}

	public MyEntity(Long id) {
		this.id = id;
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
}
