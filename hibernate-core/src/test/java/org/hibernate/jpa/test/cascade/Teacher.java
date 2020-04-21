/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cascade;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.AccessType;

@Entity
@AccessType("field")
public class Teacher {
	
	@Id @GeneratedValue
	Long id;
	
	String name;

	@OneToMany(mappedBy="primaryTeacher", cascade={CascadeType.MERGE, CascadeType.PERSIST})
	private Set<Student> students = new HashSet<Student>();

	@OneToOne(mappedBy="favoriteTeacher", cascade={CascadeType.MERGE, CascadeType.PERSIST})
	private Student favoriteStudent;
	
	public  Teacher() {
	}

	public Student getFavoriteStudent() {
		return favoriteStudent;
	}

	public void setFavoriteStudent(
			Student contributionOrBenefitParameters) {
		this.favoriteStudent = contributionOrBenefitParameters;
	}

	public Set<Student> getStudents() {
		return students;
	}

	public void setStudents(
			Set<Student> todoCollection) {
		this.students = todoCollection;
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
