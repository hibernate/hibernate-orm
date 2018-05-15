package org.hibernate.test.extralazy;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "championship")
public class Championship {

	@Id
	private int id;

	@OneToMany(cascade = CascadeType.ALL)
	@LazyCollection(LazyCollectionOption.EXTRA)
	@Where(clause = " gpa >= 4 ")
	private List<Student> students = new ArrayList<>();

	public Championship() {}

	public Championship(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<Student> getStudents() {
		return students;
	}
}
