package org.hibernate.jpa.test.cascade;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Entity
@Access(AccessType.FIELD)
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
