package org.hibernate.orm.test.extralazy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "school")
public class School {

	@Id
	private int id;

	@OneToMany(mappedBy = "school")
	@LazyCollection(LazyCollectionOption.EXTRA)
	private Set<Student> students = new HashSet<Student>();

	@OneToMany(mappedBy = "school")
	@LazyCollection(LazyCollectionOption.EXTRA)
	@SQLRestriction(" gpa >= 4 ")
	private Set<Student> topStudents = new HashSet<Student>();

	@OneToMany(mappedBy = "school")
	@LazyCollection(LazyCollectionOption.EXTRA)
	@SQLRestriction(" gpa >= 4 ")
	@MapKey
	private Map<String, Student> studentsMap = new HashMap<>();
	
	public School() {}

	public School(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Set<Student> getStudents() {
		return students;
	}

	public Set<Student> getTopStudents() {
		return topStudents;
	}

	public Map<String, Student> getStudentsMap() {
		return studentsMap;
	}
}
