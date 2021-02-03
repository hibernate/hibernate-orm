/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql.size;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.boot.SessionFactoryBuilder;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@TestForIssue(jiraKey = "HHH-13944")
public class ManyToManySizeTest2 extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		sqlStatementInterceptor = new SQLStatementInterceptor( sfb );
	}

	@Before
	public void setUp() {
		inTransaction( session -> {
			Skill mathSkill = new Skill();
			Skill frenchSkill = new Skill();

			session.persist( mathSkill );
			session.persist( frenchSkill );

			Teacher teacherWithNoSkills = new Teacher();

			Teacher teacherWithOneSkill = new Teacher();
			teacherWithOneSkill.addSkill( mathSkill );

			Teacher teacherWithTwoSkills = new Teacher();
			teacherWithTwoSkills.addSkill( mathSkill );
			teacherWithTwoSkills.addSkill( frenchSkill );

			session.persist( teacherWithNoSkills );
			session.persist( teacherWithOneSkill );
			session.persist( teacherWithTwoSkills );

			Student studentWithTeacherWithNoSkills = new Student();
			studentWithTeacherWithNoSkills.setTeacher( teacherWithNoSkills );
			session.persist( studentWithTeacherWithNoSkills );

			Student studentWithTeacherWithOneSkill = new Student();
			studentWithTeacherWithOneSkill.setTeacher( teacherWithOneSkill );
			session.persist( studentWithTeacherWithOneSkill );

			Student studentWithTeacherWithTwoSkills = new Student();
			studentWithTeacherWithTwoSkills.setTeacher( teacherWithTwoSkills );
			session.persist( studentWithTeacherWithTwoSkills );
		} );
		sqlStatementInterceptor.clear();
	}

	@After
	public void tearDown() {
		inTransaction( session -> {
			session.createQuery( "delete from Student" ).executeUpdate();
			session.createQuery( "delete from Teacher" ).executeUpdate();
			session.createQuery( "delete from Skill" ).executeUpdate();
		} );
	}

	@Test
	public void testSize() {
		inTransaction( session -> {
			List<Teacher> teachers = session.createQuery(
					"select distinct teacher from Teacher teacher join teacher.skills skills where size(skills) > 2",
					Teacher.class
			).getResultList();
			assertEquals( 0, teachers.size() );

			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher join teacher.skills skills where size(skills) > 1",
					Teacher.class
			).getResultList();
			assertEquals( 1, teachers.size() );

			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher join teacher.skills skills where size(skills) > 0",
					Teacher.class
			).getResultList();
			assertEquals( 2, teachers.size() );

			// Using "join" (instead of "left join") removes the teacher with no skills from the results.
			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher join teacher.skills skills where size(skills) > -1",
					Teacher.class
			).getResultList();
			assertEquals( 2, teachers.size() );

			// Using "left join" includes the teacher with no skills in the results.
			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join teacher.skills skills where size(skills) > -1",
					Teacher.class
			).getResultList();
			assertEquals( 3, teachers.size() );
		} );
	}

	@Test
	public void testSizeAddFetch() {
		inTransaction( session -> {
			List<Teacher> teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.skills join teacher.skills skills where size(skills) > 2",
					Teacher.class
			).getResultList();
			assertEquals( 0, teachers.size() );

			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.skills join teacher.skills skills where size(skills) > 1",
					Teacher.class
			).getResultList();
			assertEquals( 1, teachers.size() );
			assertTrue( Hibernate.isInitialized( teachers.get( 0 ).getSkills() ) );

			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.skills join teacher.skills skills where size(skills) > 0",
					Teacher.class
			).getResultList();
			assertEquals( 2, teachers.size() );
			assertTrue( Hibernate.isInitialized( teachers.get( 0 ).getSkills() ) );
			assertTrue( Hibernate.isInitialized( teachers.get( 1 ).getSkills() ) );


			// Using "join" (instead of "left join") removes the teacher with no skills from the results.
			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.skills join teacher.skills skills where size(skills) > -1",
					Teacher.class
			).getResultList();
			assertEquals( 2, teachers.size() );
			assertTrue( Hibernate.isInitialized( teachers.get( 0 ).getSkills() ) );
			assertTrue( Hibernate.isInitialized( teachers.get( 1 ).getSkills() ) );

			// Using "left join" includes the teacher with no skills in the results.
			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.skills left join teacher.skills skills where size(skills) > -1",
					Teacher.class
			).getResultList();
			assertEquals( 3, teachers.size() );
			assertTrue( Hibernate.isInitialized( teachers.get( 0 ).getSkills() ) );
			assertTrue( Hibernate.isInitialized( teachers.get( 1 ).getSkills() ) );
			assertTrue( Hibernate.isInitialized( teachers.get( 2 ).getSkills() ) );
		} );
	}

	@Test
	public void testSizeWithoutNestedPath() {
		inTransaction( session -> {
			List<Teacher> teachers = session.createQuery(
					"select teacher from Teacher teacher where size(teacher.skills) > 2",
					Teacher.class
			).getResultList();
			assertEquals( 0, teachers.size() );

			teachers = session.createQuery(
					"select teacher from Teacher teacher where size(teacher.skills) > 1",
					Teacher.class
			).getResultList();
			assertEquals( 1, teachers.size() );

			teachers = session.createQuery(
					"select teacher from Teacher teacher where size(teacher.skills) > 0",
					Teacher.class
			).getResultList();
			assertEquals( 2, teachers.size() );

			// If there is no "join", then results include the teacher with no skills
			teachers = session.createQuery(
					"select teacher from Teacher teacher where size(teacher.skills) > -1",
					Teacher.class
			).getResultList();
			assertEquals( 3, teachers.size() );
		} );
	}

	@Test
	public void testSizeWithoutNestedPathAddFetchDistinct() {
		inTransaction( session -> {
			List<Teacher> teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.skills where size(teacher.skills) > 2",
					Teacher.class
			).getResultList();
			assertEquals( countNumberOfJoins( sqlStatementInterceptor.getSqlQueries().get( 0 ) ), 2 );
			assertEquals( 0, teachers.size() );

			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.skills where size(teacher.skills) > 1",
					Teacher.class
			).getResultList();
			assertEquals( 1, teachers.size() );
			assertTrue( Hibernate.isInitialized( teachers.get( 0 ).getSkills() ) );

			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.skills where size(teacher.skills) > 0",
					Teacher.class
			).getResultList();
			assertEquals( 2, teachers.size() );
			assertTrue( Hibernate.isInitialized( teachers.get( 0 ).getSkills() ) );
			assertTrue( Hibernate.isInitialized( teachers.get( 1 ).getSkills() ) );

			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.skills where size(teacher.skills) > -1",
					Teacher.class
			).getResultList();
			assertEquals( 3, teachers.size() );
			assertTrue( Hibernate.isInitialized( teachers.get( 0 ).getSkills() ) );
			assertTrue( Hibernate.isInitialized( teachers.get( 1 ).getSkills() ) );
			assertTrue( Hibernate.isInitialized( teachers.get( 2 ).getSkills() ) );
		} );
	}

	@Test
	public void testSizeWithNestedPathAndImplicitJoin() {
		doInJPA( this::sessionFactory, em -> {
			List<Student> students = em.createQuery(
					"select student from Student student where size(student.teacher.skills) > 2",
					Student.class
			).getResultList();
			assertEquals( 0, students.size() );
			assertEquals( countNumberOfJoins( sqlStatementInterceptor.getSqlQueries().get( 0 ) ), 1 );

			students = em.createQuery(
					"select student from Student student where size(student.teacher.skills) > 1",
					Student.class
			).getResultList();
			assertEquals( 1, students.size() );

			students = em.createQuery(
					"select student from Student student where size(student.teacher.skills) > 0",
					Student.class
			).getResultList();
			assertEquals( 2, students.size() );

			// If there is no "join" in the query, then results include the student with the teacher with no skills
			students = em.createQuery(
					"select student from Student student where size(student.teacher.skills) > -1",
					Student.class
			).getResultList();
			assertEquals( 3, students.size() );
		} );
	}

	@Test
	public void testSizeWithNestedPathAndImplicitJoinAddFetchDistinct() {
		doInJPA( this::sessionFactory, em -> {
			List<Student> students = em.createQuery(
					"select distinct student from Student student left join fetch student.teacher t left join fetch t.skills where size(student.teacher.skills) > 2",
					Student.class
			).getResultList();
			assertEquals( 0, students.size() );
			assertEquals( countNumberOfJoins( sqlStatementInterceptor.getSqlQueries().get( 0 ) ), 3 );

			students = em.createQuery(
					"select distinct student from Student student left join fetch student.teacher t left join fetch t.skills where size(student.teacher.skills) > 1",
					Student.class
			).getResultList();
			assertEquals( 1, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );

			students = em.createQuery(
					"select distinct student from Student student left join fetch student.teacher t left join fetch t.skills where size(student.teacher.skills) > 0",
					Student.class
			).getResultList();
			assertEquals( 2, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );
			assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getSkills() ) );

			students = em.createQuery(
					"select distinct student from Student student left join fetch student.teacher t left join fetch t.skills where size(student.teacher.skills) > -1",
					Student.class
			).getResultList();
			assertEquals( 3, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );
			assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getSkills() ) );
			assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getSkills() ) );
		} );
	}

	@Test
	public void testSizeWithNestedPath() {
		doInJPA( this::sessionFactory, em -> {
			List<Student> students = em.createQuery(
					"select student from Student student join student.teacher t where size(student.teacher.skills) > -1",
					Student.class
			).getResultList();
			assertEquals( 3L, students.size() );
			assertEquals( countNumberOfJoins( sqlStatementInterceptor.getSqlQueries().get( 0 ) ), 1 );

			students = em.createQuery(
					"select student from Student student join student.teacher t where size(student.teacher.skills) > 0",
					Student.class
			).getResultList();
			assertEquals( 2L, students.size() );

			students = em.createQuery(
					"select student from Student student join student.teacher t where size(student.teacher.skills) > 1",
					Student.class
			).getResultList();
			assertEquals( 1L, students.size() );

			students = em.createQuery(
					"select student from Student student join student.teacher t where size(student.teacher.skills) > 2",
					Student.class
			).getResultList();
			assertEquals( 0L, students.size() );
		} );
	}

	@Test
	public void testSizeWithNestedPathAddFetchDistinct() {
		doInJPA( this::sessionFactory, em -> {
			List<Student> students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tjoin left join fetch tjoin.skills where size(student.teacher.skills) > -1",
					Student.class
			).getResultList();
			// NOTE: An INNER JOIN is done on Teacher twice, which results in 4 joins.
			//       A possible optimization would be to reuse this INNER JOIN.
			assertEquals( countNumberOfJoins( sqlStatementInterceptor.getSqlQueries().get( 0 ) ), 4 );
			assertEquals( 3L, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );
			assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getSkills() ) );
			assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getSkills() ) );

			students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tjoin left join fetch tjoin.skills where size(student.teacher.skills) > 0",
					Student.class
			).getResultList();
			assertEquals( 2L, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );
			assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getSkills() ) );

			students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tjoin left join fetch tjoin.skills where size(student.teacher.skills) > 1",
					Student.class
			).getResultList();
			assertEquals( 1L, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );

			students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tjoin left join fetch tjoin.skills where size(student.teacher.skills) > 2",
					Student.class
			).getResultList();
			assertEquals( 0L, students.size() );
		} );
	}

	@Test
	public void testSizeWithNestedPath2() {
		doInJPA( this::sessionFactory, em -> {
			List<Student> students = em.createQuery(
					"select student from Student student join student.teacher t where size(t.skills) > -1",
					Student.class
			).getResultList();
			assertEquals( 3L, students.size() );
			assertEquals( countNumberOfJoins( sqlStatementInterceptor.getSqlQueries().get( 0 ) ), 1 );

			students = em.createQuery(
					"select student from Student student join student.teacher t where size(t.skills) > 0",
					Student.class
			).getResultList();
			assertEquals( 2L, students.size() );

			students = em.createQuery(
					"select student from Student student join student.teacher t where size(t.skills) > 1",
					Student.class
			).getResultList();
			assertEquals( 1L, students.size() );

			students = em.createQuery(
					"select student from Student student join student.teacher t where size(t.skills) > 2",
					Student.class
			).getResultList();
			assertEquals( 0L, students.size() );
		} );
	}

	@Test
	public void testSizeWithNestedPath2AddFetchDistinct() {
		doInJPA( this::sessionFactory, em -> {
			List<Student> students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch left join fetch tfetch.skills where size(t.skills) > -1",
					Student.class
			).getResultList();
			// NOTE: An INNER JOIN is done on Teacher twice, which results in 4 joins.
			//       A possible optimization would be to reuse this INNER JOIN.
			assertEquals( countNumberOfJoins( sqlStatementInterceptor.getSqlQueries().get( 0 ) ), 4 );
			assertEquals( 3L, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );
			assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getSkills() ) );
			assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getSkills() ) );

			students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch left join fetch tfetch.skills where size(t.skills) > 0",
					Student.class
			).getResultList();
			assertEquals( 2L, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );
			assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getSkills() ) );

			students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch left join fetch tfetch.skills where size(t.skills) > 1",
					Student.class
			).getResultList();
			assertEquals( 1L, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );

			students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch left join fetch tfetch.skills where size(t.skills) > 2",
					Student.class
			).getResultList();
			assertEquals( 0L, students.size() );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Skill.class, Teacher.class, Student.class };
	}

	@Entity(name = "Skill")
	public static class Skill {

		private Integer id;

		private Set<Teacher> teachers = new HashSet<>();

		@Id
		@Column(name = "skill_id")
		@GeneratedValue
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@ManyToMany(mappedBy = "skills")
		public Set<Teacher> getTeachers() {
			return teachers;
		}

		public void setTeachers(Set<Teacher> teachers) {
			this.teachers = teachers;
		}

		public void addTeacher(Teacher teacher) {
			teachers.add( teacher );
		}
	}

	@Entity(name = "Teacher")
	public static class Teacher {

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

	@Entity(name = "Student")
	public static class Student {

		private Integer id;

		private Teacher teacher;

		@Id
		@Column(name = "student_id")
		@GeneratedValue
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
			teacher.addStudent( this );
		}
	}

	private static int countNumberOfJoins(String query) {
		String fromPart = query.toLowerCase( Locale.ROOT ).split( " from " )[1].split( " where " )[0];
		return fromPart.split( "(\\sjoin\\s|,\\s)", -1 ).length - 1;
	}
}
