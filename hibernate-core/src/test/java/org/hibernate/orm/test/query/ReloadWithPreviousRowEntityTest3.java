/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Jpa(
		annotatedClasses = { ReloadWithPreviousRowEntityTest3.Skill.class, ReloadWithPreviousRowEntityTest3.Student.class, ReloadWithPreviousRowEntityTest3.Teacher.class }
)
@Jira("https://hibernate.atlassian.net/browse/HHH-18271")
public class ReloadWithPreviousRowEntityTest3 {

	@BeforeEach
	public void prepareTestData(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Student mathStudent = new Student(16);
					Student frenchStudent = new Student(17);
					Student scienceStudent = new Student(18);

					Teacher teacherWithNoStudents = new Teacher(16);
					Teacher teacherWithOneStudent = new Teacher(17);
					Teacher teacherWithTwoStudents = new Teacher(18);

					session.persist( teacherWithNoStudents );
					session.persist( teacherWithOneStudent );
					session.persist( teacherWithTwoStudents );

					mathStudent.setTeacher( teacherWithOneStudent );
					teacherWithOneStudent.addStudent( mathStudent );

					frenchStudent.setTeacher( teacherWithTwoStudents );
					teacherWithTwoStudents.addStudent( frenchStudent );

					scienceStudent.setTeacher( teacherWithTwoStudents );
					teacherWithTwoStudents.addStudent( scienceStudent );

					session.persist( mathStudent );
					session.persist( frenchStudent );
					session.persist( scienceStudent );
				}
		);
	}

	@AfterEach
	public void dropTestData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testReloadWithPreviousRow(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			// First load a fully initialized graph i.e. maybeLazySet empty
			em.createQuery( "select s from Student s join fetch s.teacher t left join fetch t.students left join fetch t.skills order by s.id desc", Student.class ).getResultList();
			// Then load a partially initialized graph and see if previous row optimization works properly
			em.createQuery( "select s from Student s join fetch s.teacher t left join fetch t.students order by s.id desc", Student.class ).getResultList();
		} );
	}

	@Entity(name = "Student")
	public static class Student {
		private Integer id;

		private Teacher teacher;

		public Student() {
		}

		public Student(Integer id) {
			this.id = id;
		}

		@Id
		@Column(name = "student_id")
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@ManyToOne(optional = false)
		@JoinColumn(name = "teacher_fk_id")
		public Teacher getTeacher() {
			return teacher;
		}

		public void setTeacher(Teacher teacher) {
			this.teacher = teacher;
		}

	}

	@Entity(name = "Teacher")
	public static class Teacher {

		private Integer id;

		private Set<Student> students = new HashSet<>();

		private Set<Skill> skills = new HashSet<>();

		public Teacher() {
		}

		public Teacher(Integer id) {
			this.id = id;
		}

		@Id
		@Column(name = "teacher_id")
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
		}

		public void setSkills(Set<Skill> skills) {
			this.skills = skills;
		}
	}

	@Entity(name = "Skill")
	public static class Skill {

		private Integer id;
		private String name;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
