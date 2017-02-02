package org.hibernate.test.extralazy;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "student")
public class Student {

	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne
	private School school;

	private String firstName;

	private int gpa;
	
	public Student() {}

	public Student(String firstName, int gpa) {
		this.firstName = firstName;
		this.gpa = gpa;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public School getSchool() {
		return school;
	}

	public void setSchool(School school) {
		this.school = school;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public int getGpa() {
		return gpa;
	}

	public void setGpa(int gpa) {
		this.gpa = gpa;
	}


	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof Student ) ) {
			return false;
		}
		Student student = (Student) o;
		return Objects.equals( getFirstName(), student.getFirstName() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( getFirstName() );
	}
}
