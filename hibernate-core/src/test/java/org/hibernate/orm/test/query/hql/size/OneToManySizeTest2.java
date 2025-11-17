/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.size;

import java.util.List;
import java.util.Locale;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hibernate.testing.jdbc.SQLStatementInspector.extractFromSession;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@JiraKey(value = "HHH-13944")
@DomainModel(
		annotatedClasses = { Skill.class, Teacher.class, Student.class }
)
@SessionFactory( useCollectingStatementInspector = true )
public class OneToManySizeTest2 {
	@Test
	public void testSize(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
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
				}
			);
	}

	@Test
	public void testSizeAddFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
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
				}
			);
	}

	@Test
	public void testSizeWithoutNestedPath(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
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
				}
			);
	}

	@Test
	public void testSizeWithoutNestedPathAddFetchDistinct(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final SQLStatementInspector statementInspector = extractFromSession( session );
					statementInspector.clear();

					List<Teacher> teachers = session.createQuery(
							"select distinct teacher from Teacher teacher left join fetch teacher.students where size(teacher.students) > 2",
							Teacher.class
					).getResultList();
					assertEquals( countNumberOfJoins( statementInspector.getSqlQueries().get( 0 ) ), 1 );
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
				}
			);
	}

	@Test
	public void testSizeWithNestedPathAndImplicitJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final SQLStatementInspector statementInspector = extractFromSession( session );
					statementInspector.clear();

					List<Student> students = session.createQuery(
							"select student from Student student where size(student.teacher.students) > 2",
							Student.class
					).getResultList();
					assertEquals( 0, students.size() );
					assertEquals( countNumberOfJoins( statementInspector.getSqlQueries().get( 0 ) ), 1 );

					students = session.createQuery(
							"select student from Student student where size(student.teacher.students) > 1",
							Student.class
					).getResultList();
					assertEquals( 2, students.size() );

					students = session.createQuery(
							"select student from Student student where size(student.teacher.students) > 0",
							Student.class
					).getResultList();
					assertEquals( 3, students.size() );

					students = session.createQuery(
							"select student from Student student where size(student.teacher.students) > -1",
							Student.class
					).getResultList();
					assertEquals( 3, students.size() );
				}
		);
	}

	@Test
	public void testCollectionFetchBaseline(SessionFactoryScope scope) {
		// tests various "normal" ways to use a fetch
		scope.inTransaction(
				(session) -> {

					final List list = session.createQuery( "from Teacher t join fetch t.students" ).list();
					assertThat( list.size(), is( 2 ) );
				}
		);
	}


	@Test
	public void testSizeWithNestedPathAndImplicitJoinAddFetchDistinct(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final SQLStatementInspector statementInspector = extractFromSession( session );
					statementInspector.clear();

					List<Student> students = session.createQuery(
							"select distinct student " +
									"from Student student " +
									"	left join fetch student.teacher t " +
									"	left join fetch t.students " +
									"where size(student.teacher.students) > 2",
							Student.class
					).getResultList();
					assertEquals( 2, countNumberOfJoins( statementInspector.getSqlQueries().get( 0 ) ) );
					assertEquals( 0, students.size() );

					students = session.createQuery(
							"select distinct student " +
									"from Student student " +
									"	left join fetch student.teacher t " +
									"	left join fetch t.students " +
									"where size(student.teacher.students) > 1",
							Student.class
					).getResultList();
					assertEquals( 2, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
					assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );

					students = session.createQuery(
							"select distinct student " +
									"from Student student " +
									"	left join fetch student.teacher t " +
									"	left join fetch t.students " +
									"where size(student.teacher.students) > 0",
							Student.class
					).getResultList();
					assertEquals( 3, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
					assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );
					assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getStudents() ) );

					students = session.createQuery(
							"select distinct student " +
									"from Student student " +
									"	left join fetch student.teacher t " +
									"	left join fetch t.students " +
									"where size(student.teacher.students) > -1",
							Student.class
					).getResultList();
					assertEquals( 3, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
					assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );
					assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getStudents() ) );
				}
		);
	}

	@Test
	public void testSizeWithNestedPathBase(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final SQLStatementInspector statementInterceptor = extractFromSession( session );
					statementInterceptor.clear();

					List<Student> students = null;

					students = session.createQuery(
							"select student from Student student join student.teacher t",
							Student.class
					).getResultList();
					assertEquals( 3L, students.size() );
					// Since the join for "student.teacher" is never used and is a non-optional association we don't generate a SQL join for it
					assertEquals( 0, countNumberOfJoins( statementInterceptor.getSqlQueries().get( 0 ) ) );

					statementInterceptor.clear();

					students = session.createQuery(
							"select student from Student student join student.teacher t where size( t.students ) > -1",
							Student.class
					).getResultList();
					assertEquals( 1, countNumberOfJoins( statementInterceptor.getSqlQueries().get( 0 ) ) );

					statementInterceptor.clear();

					students = session.createQuery(
							"select student from Student student join student.teacher where size( student.teacher.students ) > -1",
							Student.class
					).getResultList();
					// Since the join for "student.teacher" is never used and is a non-optional association we don't generate a SQL join for it
					statementInterceptor.assertNumberOfJoins( 0, 1 );

				}
		);
	}

	@Test
	public void testSizeWithNestedPath(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final SQLStatementInspector statementInspector = extractFromSession( session );
					statementInspector.clear();

					List<Student> students = session.createQuery(
							"select student from Student student join student.teacher t where size(student.teacher.students) > -1",
							Student.class
					).getResultList();
					assertEquals( 3L, students.size() );
					// Since the join for "student.teacher" is never used and is a non-optional association we don't generate a SQL join for it
					assertEquals( 1, countNumberOfJoins( statementInspector.getSqlQueries().get( 0 ) ) );

					students = session.createQuery(
							"select student from Student student join student.teacher t where size(student.teacher.students) > 0",
							Student.class
					).getResultList();
					assertEquals( 3L, students.size() );

					students = session.createQuery(
							"select student from Student student join student.teacher t where size(student.teacher.students) > 1",
							Student.class
					).getResultList();
					assertEquals( 2L, students.size() );

					students = session.createQuery(
							"select student from Student student join student.teacher t where size(student.teacher.students) > 2",
							Student.class
					).getResultList();
					assertEquals( 0L, students.size() );
				}
		);
	}

	@Test
	public void testSizeWithNestedPathAddFetchDistinctBase(SessionFactoryScope scope) {

		scope.inTransaction(
				(session) -> {
					final SQLStatementInspector statementInspector = extractFromSession( session );
					statementInspector.clear();

					{
						List<Student> students = session.createQuery(
								"select distinct student " +
										"from Student student " +
										"	join student.teacher t " +
										"	join fetch student.teacher tjoin " +
										"	left join fetch tjoin.students " +
										"where size(tjoin.students) > -1",
								Student.class
						).getResultList();
						// Since the join for "student.teacher" is never used and is a non-optional association we don't generate a SQL join for it
						assertEquals( 2, countNumberOfJoins( statementInspector.getSqlQueries().get( 0 ) ) );
						assertEquals( 3L, students.size() );
					}

					{
						List<Student> students = session.createQuery(
								"select distinct student " +
										"from Student student " +
										"	join student.teacher t " +
										"	join fetch student.teacher tjoin " +
										"	left join fetch tjoin.students " +
										"where size(t.students) > -1",
								Student.class
						).getResultList();
						// Since the join for "student.teacher" is never used and is a non-optional association we don't generate a SQL join for it
						assertEquals( 2, countNumberOfJoins( statementInspector.getSqlQueries().get( 0 ) ) );
						assertEquals( 3L, students.size() );
					}
				}
		);
	}

	@Test
	public void testSizeWithNestedPathAddFetchDistinct(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final SQLStatementInspector statementInspector = extractFromSession( session );
					statementInspector.clear();

					List<Student> students;
					students = session.createQuery(
							"select distinct student " +
									"from Student student " +
									"	join student.teacher t " +
									"	join fetch student.teacher tjoin " +
									"	left join fetch tjoin.students " +
									"where size(student.teacher.students) > -1",
							Student.class
					).getResultList();
					// Since the join for "student.teacher" is never used and is a non-optional association we don't generate a SQL join for it
					assertEquals( 2, countNumberOfJoins( statementInspector.getSqlQueries().get( 0 ) ) );
					assertEquals( 3L, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
					assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );
					assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getStudents() ) );

					students = session.createQuery(
							"select distinct student " +
									"from Student student " +
									"	join student.teacher t " +
									"	join fetch student.teacher tjoin " +
									"	left join fetch tjoin.students " +
									"where size(student.teacher.students) > 0",
							Student.class
					).getResultList();
					// Since the join for "student.teacher" is never used and is a non-optional association we don't generate a SQL join for it
					assertEquals( 2, countNumberOfJoins( statementInspector.getSqlQueries().get( 0 ) ) );
					assertEquals( 3L, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
					assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );
					assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getStudents() ) );

					students = session.createQuery(
							"select distinct student " +
									"from Student student " +
									"	join student.teacher t " +
									"	join fetch student.teacher tjoin " +
									"	left join fetch tjoin.students " +
									"where size(student.teacher.students) > 1",
							Student.class
					).getResultList();
					// Since the join for "student.teacher" is never used and is a non-optional association we don't generate a SQL join for it
					assertEquals( 2, countNumberOfJoins( statementInspector.getSqlQueries().get( 0 ) ) );
					assertEquals( 2L, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
					assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );

					students = session.createQuery(
							"select distinct student " +
									"from Student student " +
									"	join student.teacher t " +
									"	join fetch student.teacher tjoin " +
									"	left join fetch tjoin.students " +
									"where size(student.teacher.students) > 2",
							Student.class
					).getResultList();
					assertEquals( 0L, students.size() );
				}
		);
	}

	@Test
	public void testSizeWithNestedPath2(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final SQLStatementInspector statementInspector = extractFromSession( session );
					statementInspector.clear();

					List<Student> students = session.createQuery(
							"select student from Student student join student.teacher t where size(t.students) > -1",
							Student.class
					).getResultList();
					assertEquals( 3L, students.size() );
					assertEquals( countNumberOfJoins( statementInspector.getSqlQueries().get( 0 ) ), 1 );

					students = session.createQuery(
							"select student from Student student join student.teacher t where size(t.students) > 0",
							Student.class
					).getResultList();
					assertEquals( 3L, students.size() );

					students = session.createQuery(
							"select student from Student student join student.teacher t where size(t.students) > 1",
							Student.class
					).getResultList();
					assertEquals( 2L, students.size() );

					students = session.createQuery(
							"select student from Student student join student.teacher t where size(t.students) > 2",
							Student.class
					).getResultList();
					assertEquals( 0L, students.size() );
				}
		);
	}

	@Test
	public void testSizeWithNestedPath2AddFetchDistinct(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final SQLStatementInspector statementInspector = extractFromSession( session );
					statementInspector.clear();

					List<Student> students = session.createQuery(
							"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch join fetch tfetch.students where size(t.students) > -1",
							Student.class
					).getResultList();
					assertEquals( 3, countNumberOfJoins( statementInspector.getSqlQueries().get( 0 ) ) );
					assertEquals( 3L, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
					assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );
					assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getStudents() ) );

					students = session.createQuery(
							"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch join fetch tfetch.students where size(t.students) > 0",
							Student.class
					).getResultList();
					assertEquals( 3L, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
					assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );
					assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getStudents() ) );

					students = session.createQuery(
							"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch join fetch tfetch.students where size(t.students) > 1",
							Student.class
					).getResultList();
					assertEquals( 2L, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getStudents() ) );
					assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getStudents() ) );

					students = session.createQuery(
							"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch join fetch tfetch.students where size(t.students) > 2",
							Student.class
					).getResultList();
					assertEquals( 0L, students.size() );
				}
		);
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
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

					final SQLStatementInspector statementInspector = extractFromSession( session );
					statementInspector.clear();
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	private static int countNumberOfJoins(String query) {
		String fromPart = query.toLowerCase( Locale.ROOT ).split( " from " )[1].split( " where " )[0];
		return fromPart.split( "(\\sjoin\\s|,\\s)", -1 ).length - 1;
	}

}
