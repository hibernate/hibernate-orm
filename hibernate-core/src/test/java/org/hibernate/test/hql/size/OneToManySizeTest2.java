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
public class OneToManySizeTest2 extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		sqlStatementInterceptor = new SQLStatementInterceptor( sfb );
	}

	@Before
	public void setUp() {
		inTransaction( session -> {
			Student mathStudent = new Student();
			Student frenchStudent = new Student();
			Student scienceStudent = new Student();

			Teacher teacherWithNoStudents = new Teacher();
			Teacher teacherWithOneStudent = new Teacher();
			Teacher teacherWithTwoStudents = new Teacher();

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
					"select distinct teacher from Teacher teacher join teacher.students students where size(students) > 2",
					Teacher.class
			).getResultList();
			assertEquals( 0, teachers.size() );

			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher join teacher.students students where size(students) > 1",
					Teacher.class
			).getResultList();
			assertEquals( 1, teachers.size() );

			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher join teacher.students students where size(students) > 0",
					Teacher.class
			).getResultList();
			assertEquals( 2, teachers.size() );

			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher join teacher.students students where size(students) > -1",
					Teacher.class
			).getResultList();
			assertEquals( 2, teachers.size() );

			// Using "left join" includes the teacher with no students in the results.
			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join teacher.students students where size(students) > -1",
					Teacher.class
			).getResultList();
			assertEquals( 3, teachers.size() );
		} );
	}

	@Test
	public void testSizeAddFetch() {
		inTransaction( session -> {
			List<Teacher> teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.students join teacher.students students where size(students) > 2",
					Teacher.class
			).getResultList();
			assertEquals( 0, teachers.size() );

			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.students join teacher.students students where size(students) > 1",
					Teacher.class
			).getResultList();
			assertEquals( 1, teachers.size() );
			assertTrue( Hibernate.isInitialized( teachers.get( 0 ).getStudents() ) );

			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.students join teacher.students students where size(students) > 0",
					Teacher.class
			).getResultList();
			assertEquals( 2, teachers.size() );
			assertTrue( Hibernate.isInitialized( teachers.get( 0 ).getStudents() ) );
			assertTrue( Hibernate.isInitialized( teachers.get( 1 ).getStudents() ) );


			// Using "join" (instead of "left join") removes the teacher with no students from the results.
			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.students join teacher.students students where size(students) > -1",
					Teacher.class
			).getResultList();
			assertEquals( 2, teachers.size() );
			assertTrue( Hibernate.isInitialized( teachers.get( 0 ).getStudents() ) );
			assertTrue( Hibernate.isInitialized( teachers.get( 1 ).getStudents() ) );

			// Using "left join" includes the teacher with no students in the results.
			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.students left join teacher.students students where size(students) > -1",
					Teacher.class
			).getResultList();
			assertEquals( 3, teachers.size() );
			assertTrue( Hibernate.isInitialized( teachers.get( 0 ).getStudents() ) );
			assertTrue( Hibernate.isInitialized( teachers.get( 1 ).getStudents() ) );
			assertTrue( Hibernate.isInitialized( teachers.get( 2 ).getStudents() ) );
		} );
	}

	@Test
	public void testSizeWithoutNestedPath() {
		inTransaction( session -> {
			List<Teacher> teachers = session.createQuery(
					"select teacher from Teacher teacher where size(teacher.students) > 2",
					Teacher.class
			).getResultList();
			assertEquals( 0, teachers.size() );

			teachers = session.createQuery(
					"select teacher from Teacher teacher where size(teacher.students) > 1",
					Teacher.class
			).getResultList();
			assertEquals( 1, teachers.size() );

			teachers = session.createQuery(
					"select teacher from Teacher teacher where size(teacher.students) > 0",
					Teacher.class
			).getResultList();
			assertEquals( 2, teachers.size() );

			// If there is no "join", then results include the teacher with no students
			teachers = session.createQuery(
					"select teacher from Teacher teacher where size(teacher.students) > -1",
					Teacher.class
			).getResultList();
			assertEquals( 3, teachers.size() );
		} );
	}

	@Test
	public void testSizeWithoutNestedPathAddFetchDistinct() {
		inTransaction( session -> {
			List<Teacher> teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.students where size(teacher.students) > 2",
					Teacher.class
			).getResultList();
			assertEquals( countNumberOfJoins( sqlStatementInterceptor.getSqlQueries().get( 0 ) ), 1 );
			assertEquals( 0, teachers.size() );

			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.students where size(teacher.students) > 1",
					Teacher.class
			).getResultList();
			assertEquals( 1, teachers.size() );
			assertTrue( Hibernate.isInitialized( teachers.get( 0 ).getStudents() ) );

			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.students where size(teacher.students) > 0",
					Teacher.class
			).getResultList();
			assertEquals( 2, teachers.size() );
			assertTrue( Hibernate.isInitialized( teachers.get( 0 ).getStudents() ) );
			assertTrue( Hibernate.isInitialized( teachers.get( 1 ).getStudents() ) );

			teachers = session.createQuery(
					"select distinct teacher from Teacher teacher left join fetch teacher.students where size(teacher.students) > -1",
					Teacher.class
			).getResultList();
			assertEquals( 3, teachers.size() );
			assertTrue( Hibernate.isInitialized( teachers.get( 0 ).getStudents() ) );
			assertTrue( Hibernate.isInitialized( teachers.get( 1 ).getStudents() ) );
			assertTrue( Hibernate.isInitialized( teachers.get( 2 ).getStudents() ) );
		} );
	}

	@Test
	public void testSizeWithNestedPathAndImplicitJoin() {
		doInJPA( this::sessionFactory, em -> {
			List<Student> students = em.createQuery(
					"select student from Student student where size(student.teacher.students) > 2",
					Student.class
			).getResultList();
			assertEquals( 0, students.size() );
			assertEquals( countNumberOfJoins( sqlStatementInterceptor.getSqlQueries().get( 0 ) ), 1 );

			students = em.createQuery(
					"select student from Student student where size(student.teacher.students) > 1",
					Student.class
			).getResultList();
			assertEquals( 2, students.size() );

			students = em.createQuery(
					"select student from Student student where size(student.teacher.students) > 0",
					Student.class
			).getResultList();
			assertEquals( 3, students.size() );

			students = em.createQuery(
					"select student from Student student where size(student.teacher.students) > -1",
					Student.class
			).getResultList();
			assertEquals( 3, students.size() );
		} );
	}

	@Test
	public void testSizeWithNestedPathAndImplicitJoinAddFetchDistinct() {
		doInJPA( this::sessionFactory, em -> {
			List<Student> students = em.createQuery(
					"select distinct student from Student student left join fetch student.teacher t left join fetch t.students where size(student.teacher.students) > 2",
					Student.class
			).getResultList();
			assertEquals( 0, students.size() );
			assertEquals( countNumberOfJoins( sqlStatementInterceptor.getSqlQueries().get( 0 ) ), 2 );

			students = em.createQuery(
					"select distinct student from Student student left join fetch student.teacher t left join fetch t.students where size(student.teacher.students) > 1",
					Student.class
			).getResultList();
			assertEquals( 2, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
			assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );

			students = em.createQuery(
					"select distinct student from Student student left join fetch student.teacher t left join fetch t.students where size(student.teacher.students) > 0",
					Student.class
			).getResultList();
			assertEquals( 3, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
			assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );
			assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getStudents() ) );

			students = em.createQuery(
					"select distinct student from Student student left join fetch student.teacher t left join fetch t.students where size(student.teacher.students) > -1",
					Student.class
			).getResultList();
			assertEquals( 3, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
			assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );
			assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getStudents() ) );
		} );
	}

	@Test
	public void testSizeWithNestedPath() {
		doInJPA( this::sessionFactory, em -> {
			List<Student> students = em.createQuery(
					"select student from Student student join student.teacher t where size(student.teacher.students) > -1",
					Student.class
			).getResultList();
			assertEquals( 3L, students.size() );
			assertEquals( countNumberOfJoins( sqlStatementInterceptor.getSqlQueries().get( 0 ) ), 1 );

			students = em.createQuery(
					"select student from Student student join student.teacher t where size(student.teacher.students) > 0",
					Student.class
			).getResultList();
			assertEquals( 3L, students.size() );

			students = em.createQuery(
					"select student from Student student join student.teacher t where size(student.teacher.students) > 1",
					Student.class
			).getResultList();
			assertEquals( 2L, students.size() );

			students = em.createQuery(
					"select student from Student student join student.teacher t where size(student.teacher.students) > 2",
					Student.class
			).getResultList();
			assertEquals( 0L, students.size() );
		} );
	}

	@Test
	public void testSizeWithNestedPathAddFetchDistinct() {
		doInJPA( this::sessionFactory, em -> {
			List<Student> students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tjoin left join fetch tjoin.students where size(student.teacher.students) > -1",
					Student.class
			).getResultList();
			// NOTE: An INNER JOIN is done on Teacher twice, which results in 4 joins.
			//       A possible optimization would be to reuse this INNER JOIN.
			assertEquals( countNumberOfJoins( sqlStatementInterceptor.getSqlQueries().get( 0 ) ), 3 );
			assertEquals( 3L, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
			assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );
			assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getStudents() ) );

			students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tjoin left join fetch tjoin.students where size(student.teacher.students) > 0",
					Student.class
			).getResultList();
			assertEquals( 3L, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
			assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );
			assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getStudents() ) );

			students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tjoin left join fetch tjoin.students where size(student.teacher.students) > 1",
					Student.class
			).getResultList();
			assertEquals( 2L, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
			assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );

			students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tjoin left join fetch tjoin.students where size(student.teacher.students) > 2",
					Student.class
			).getResultList();
			assertEquals( 0L, students.size() );
		} );
	}

	@Test
	public void testSizeWithNestedPath2() {
		doInJPA( this::sessionFactory, em -> {
			List<Student> students = em.createQuery(
					"select student from Student student join student.teacher t where size(t.students) > -1",
					Student.class
			).getResultList();
			assertEquals( 3L, students.size() );
			assertEquals( countNumberOfJoins( sqlStatementInterceptor.getSqlQueries().get( 0 ) ), 1 );

			students = em.createQuery(
					"select student from Student student join student.teacher t where size(t.students) > 0",
					Student.class
			).getResultList();
			assertEquals( 3L, students.size() );

			students = em.createQuery(
					"select student from Student student join student.teacher t where size(t.students) > 1",
					Student.class
			).getResultList();
			assertEquals( 2L, students.size() );

			students = em.createQuery(
					"select student from Student student join student.teacher t where size(t.students) > 2",
					Student.class
			).getResultList();
			assertEquals( 0L, students.size() );
		} );
	}

	@Test
	public void testSizeWithNestedPath2AddFetchDistinct() {
		doInJPA( this::sessionFactory, em -> {
			List<Student> students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch join fetch tfetch.students where size(t.students) > -1",
					Student.class
			).getResultList();
			// NOTE: An INNER JOIN is done on Teacher twice, which results in 4 joins.
			//       A possible optimization would be to reuse this INNER JOIN.
			assertEquals( countNumberOfJoins( sqlStatementInterceptor.getSqlQueries().get( 0 ) ), 3 );
			assertEquals( 3L, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
			assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );
			assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getStudents() ) );

			students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch join fetch tfetch.students where size(t.students) > 0",
					Student.class
			).getResultList();
			assertEquals( 3L, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
			assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );
			assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getStudents() ) );

			students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch join fetch tfetch.students where size(t.students) > 1",
					Student.class
			).getResultList();
			assertEquals( 2L, students.size() );
			assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
			assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );

			students = em.createQuery(
					"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch join fetch tfetch.students where size(t.students) > 2",
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
		}
	}

	private static int countNumberOfJoins(String query) {
		String fromPart = query.toLowerCase( Locale.ROOT ).split( " from " )[1].split( " where " )[0];
		return fromPart.split( "(\\sjoin\\s|,\\s)", -1 ).length - 1;
	}
}
