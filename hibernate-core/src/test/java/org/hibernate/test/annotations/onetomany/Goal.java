/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetomany;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

/**
 * @author Stephen Fikes
 */
@Entity
public class Goal {
	@Id
	private String name;

	@OneToMany
	@JoinColumn(name = "goal_id", table = "task_base")
	private Set<Task> tasks = new HashSet<Task>();

	@OneToMany
	@JoinColumn(name = "goal_id", table = "task_base")
	private Set<OtherTask> otherTasks = new HashSet<OtherTask>();


	public Goal(String name) {
		this();
		setName(name);
	}

	public String getName() {
		return name;
	}

	protected Goal() {
		// this form used by Hibernate
	}

	protected void setName(String name) {
		this.name = name;
	}

	public Set<Task> getTasks() {
		return tasks;
	}

	public void setTasks(Set<Task> tasks) {
		this.tasks = tasks;
	}

	public Set<OtherTask> getOtherTasks() {
		return otherTasks;
	}

	public void setOtherTasks(Set<OtherTask> tasks) {
		this.otherTasks = otherTasks;
	}

}
