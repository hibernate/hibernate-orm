/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Gail Badner
 */
public class EntityWithFunctionAsColumnHolder {
	private long id;
	private EntityWithFunctionAsColumnHolder nextHolder;
	private Set entityWithArgFunctionAsColumns = new HashSet();
	private Set entityWithNoArgFunctionAsColumns = new HashSet();

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

	public EntityWithFunctionAsColumnHolder getNextHolder() {
		return nextHolder;
	}
	public void setNextHolder(EntityWithFunctionAsColumnHolder nextHolder) {
		this.nextHolder = nextHolder;
	}

	public Set getEntityWithArgFunctionAsColumns() {
		return entityWithArgFunctionAsColumns;
	}
	public void setEntityWithArgFunctionAsColumns(Set entityWithArgFunctionAsColumns) {
		this.entityWithArgFunctionAsColumns = entityWithArgFunctionAsColumns;
	}

	public Set getEntityWithNoArgFunctionAsColumns() {
		return entityWithNoArgFunctionAsColumns;
	}
	public void setEntityWithNoArgFunctionAsColumns(Set entityWithNoArgFunctionAsColumns) {
		this.entityWithNoArgFunctionAsColumns = entityWithNoArgFunctionAsColumns;
	}
}
