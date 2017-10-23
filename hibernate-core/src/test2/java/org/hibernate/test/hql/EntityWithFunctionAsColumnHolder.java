/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;
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
