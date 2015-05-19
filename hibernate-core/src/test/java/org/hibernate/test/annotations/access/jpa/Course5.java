/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: AccessTest.java 15025 2008-08-11 09:14:39Z hardy.ferentschik $

package org.hibernate.test.annotations.access.jpa;
import java.util.List;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;


/**
 * @author Hardy Ferentschik
 */
@Entity
@Access(AccessType.PROPERTY)
public class Course5 {

	@Access(AccessType.FIELD)
	@Id
	@GeneratedValue
	private long id;

	private String title;

	private List<Student> students;


	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@ManyToMany
	public List<Student> getStudents() {
		return students;
	}

	public void setStudents(List<Student> students) {
		this.students = students;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
