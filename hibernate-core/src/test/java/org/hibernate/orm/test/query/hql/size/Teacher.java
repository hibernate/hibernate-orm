/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.hql.size;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

/**
 * @author Steve Ebersole
 */
@Entity
public class Teacher {

	private Integer id;

	private Set<Student> students = new HashSet<>();

	private Set<Skill> skills = new HashSet<>();

	@Id
	@Column(name = "teacher_id")
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToMany(mappedBy = "teacher")
	public Set<Student> getStudents() {
		return students;
	}

	public void setStudents(Set<Student> students) {
		this.students = students;
	}

	public void addStudent(Student student) {
		students.add( student );
	}

	@ManyToMany
	public Set<Skill> getSkills() {
		return skills;
	}

	public void addSkill(Skill skill) {
		skills.add( skill );
		skill.addTeacher( this );
	}

	public void setSkills(Set<Skill> skills) {
		this.skills = skills;
	}
}
