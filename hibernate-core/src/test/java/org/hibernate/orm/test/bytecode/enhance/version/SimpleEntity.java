/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhance.version;

import org.hibernate.bytecode.enhance.spi.EnhancementInfo;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.ManagedEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "SimpleEntity")
@Table(name = "SimpleEntity")
@EnhancementInfo(version = "5.3.0.Final")
public class SimpleEntity implements ManagedEntity {
	@Id
	private Integer id;
	private String name;

	@Override
	public Object $$_hibernate_getEntityInstance() {
		return null;
	}

	@Override
	public EntityEntry $$_hibernate_getEntityEntry() {
		return null;
	}

	@Override
	public void $$_hibernate_setEntityEntry(EntityEntry entityEntry) {

	}

	@Override
	public ManagedEntity $$_hibernate_getPreviousManagedEntity() {
		return null;
	}

	@Override
	public void $$_hibernate_setPreviousManagedEntity(ManagedEntity previous) {

	}

	@Override
	public ManagedEntity $$_hibernate_getNextManagedEntity() {
		return null;
	}

	@Override
	public void $$_hibernate_setNextManagedEntity(ManagedEntity next) {

	}

	@Override
	public void $$_hibernate_setUseTracker(boolean useTracker) {

	}

	@Override
	public boolean $$_hibernate_useTracker() {
		return false;
	}

	@Override
	public int $$_hibernate_getInstanceId() {
		return 0;
	}

	@Override
	public void $$_hibernate_setInstanceId(int id) {

	}
}
