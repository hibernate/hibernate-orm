/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
@NamedEntityGraphs({
	@NamedEntityGraph(
		name = "Student.Full",
		attributeNodes = {
			@NamedAttributeNode(value = Student_.COURSES)
		}
	)
})
@NamedQueries({
	@NamedQuery(name="LIST_OF_STD", query="select std from Student std")
})
public class Student {
	@Id
	private int id;
	
	private String name;
	
	@ManyToMany(cascade=CascadeType.PERSIST)
	@JoinTable(
		name="STUDENT_COURSES",
		joinColumns=@JoinColumn(referencedColumnName="ID", name="STUDENT_ID"),
		inverseJoinColumns=@JoinColumn(referencedColumnName="ID", name="COURSE_ID"),
		uniqueConstraints={@UniqueConstraint(columnNames={"STUDENT_ID", "COURSE_ID"})}
	)
	private Set<Course> courses;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Set<Course> getCourses() {
		return courses;
	}

	public void setCourses(Set<Course> courses) {
		this.courses = courses;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Student [name=" + name + "]";
	}
}
