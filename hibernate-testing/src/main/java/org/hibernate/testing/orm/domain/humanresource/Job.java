/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.humanresource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Nathan Xu
 */
@Entity
public class Job {
	private String id;
	private String title;
	private Integer minSalary;
	private Integer maxSalary;

	public Job() {
	}

	public Job(String id, String title, Integer minSalary, Integer maxSalary) {
		this.id = id;
		this.title = title;
		this.minSalary = minSalary;
		this.maxSalary = maxSalary;
	}

	@Id
	@Column( name = "job_id", length = 10 )
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Column( name = "job_title", nullable = false, length = 35 )
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Column( name = "min_salary", precision = 6 )
	public Integer getMinSalary() {
		return minSalary;
	}

	public void setMinSalary(Integer minSalary) {
		this.minSalary = minSalary;
	}

	@Column( name = "max_salary", precision = 6 )
	public Integer getMaxSalary() {
		return maxSalary;
	}

	public void setMaxSalary(Integer maxSalary) {
		this.maxSalary = maxSalary;
	}
}
