/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetomany;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * @author Stephen Fikes
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "task_base")
public class TaskBase {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ID_GENERATOR")
        @SequenceGenerator(name = "ID_GENERATOR", sequenceName = "TASK_SEQ", initialValue= 1, allocationSize = 1)
        private long id;

	private String goal_id;

	public long getId() {
		return id;
	}

	protected void setId(long id) {
		this.id = id;
	}

	public String getGoal_id() {
		return goal_id;
	}

	public void setGoal_id(String goal_id) {
		this.goal_id = goal_id;
	}
}
