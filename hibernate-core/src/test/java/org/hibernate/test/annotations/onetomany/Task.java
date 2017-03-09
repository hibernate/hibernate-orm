/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetomany;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Stephen Fikes
 */
@Entity
@Table(name = "task")
public class Task extends TaskBase {
	private String name;

	public Task(String name) {
		this();
		setName(name);
	}

	public String getName() {
		return name;
	}

	protected Task() {
		super();
		// this form used by Hibernate
	}

	protected void setName(String name) {
		this.name = name;
	}

	public int hashCode() {
		if (name != null) {
			return name.hashCode();
		} else {
			return 0;
		}
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o instanceof Task) {
			Task other = Task.class.cast(o);
			if (name != null) {
				return getName().equals(other.getName()) /* && getId() == other.getId() */;
			} else {
				return other.getName() == null;
			}
		} else {
			return false;
		}
	}
}
