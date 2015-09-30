/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.basic;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;

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

	@Id
	private Long id;

	private String name;

	public MyEntity() {
	}

	public MyEntity(Long id) {
		this.id = id;
	}

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
