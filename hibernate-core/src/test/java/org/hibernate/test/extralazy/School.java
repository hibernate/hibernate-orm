package org.hibernate.test.extralazy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Where;

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
	@Where(clause = " gpa >= 4 ")
	private Set<Student> topStudents = new HashSet<Student>();

	@OneToMany(mappedBy = "school")
	@LazyCollection(LazyCollectionOption.EXTRA)
	@Where(clause = " gpa >= 4 ")
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
