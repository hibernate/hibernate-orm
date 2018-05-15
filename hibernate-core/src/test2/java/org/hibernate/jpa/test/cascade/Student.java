/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cascade;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.hibernate.annotations.AccessType;

@Entity
@AccessType("field")
public class Student {

	@Id @GeneratedValue
	Long id;
	
	String name;
	
	@ManyToOne(cascade={CascadeType.MERGE, CascadeType.PERSIST})
	private Teacher primaryTeacher;

	@OneToOne(cascade={CascadeType.MERGE, CascadeType.PERSIST})
	private Teacher favoriteTeacher;
	
	public  Student() {
	}

	public Teacher getFavoriteTeacher() {
		return favoriteTeacher;
	}

	public void setFavoriteTeacher(Teacher lifeCover) {
		this.favoriteTeacher = lifeCover;
	}

	public Teacher getPrimaryTeacher() {
		return primaryTeacher;
	}

	public void setPrimaryTeacher(Teacher relativeTo) {
		this.primaryTeacher = relativeTo;
	}
	
	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
