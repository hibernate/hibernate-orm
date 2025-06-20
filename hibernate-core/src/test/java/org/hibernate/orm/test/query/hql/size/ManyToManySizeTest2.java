/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.size;

import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.jdbc.SQLStatementInspector.extractFromSession;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@JiraKey(value = "HHH-13944")
//@ServiceRegistry(
//		settings = @Setting( name = AvailableSettings.STATEMENT_INSPECTOR, value = "org.hibernate.testing.jdbc.SQLStatementInspector" )
//)
@DomainModel( annotatedClasses = { Skill.class, Teacher.class, Student.class } )
@SessionFactory
public class ManyToManySizeTest2 {

	@Test
	public void testSize(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
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
				}
		);
	}

	@Test
	public void testSizeAddFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
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
				}
		);
	}

	@Test
	public void testSizeWithoutNestedPath(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
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
							"select distinct teacher from Teacher teacher left join fetch teacher.skills where size(teacher.skills) > 2",
							Teacher.class
					).getResultList();
					statementInspector.assertNumberOfJoins( 0, 2 );
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
							"select student from Student student where size(student.teacher.skills) > 2",
							Student.class
					).getResultList();
					assertEquals( 0, students.size() );
					statementInspector.assertNumberOfJoins( 0, 1 );

					students = session.createQuery(
							"select student from Student student where size(student.teacher.skills) > 1",
							Student.class
					).getResultList();
					assertEquals( 1, students.size() );

					students = session.createQuery(
							"select student from Student student where size(student.teacher.skills) > 0",
							Student.class
					).getResultList();
					assertEquals( 2, students.size() );

					// If there is no "join" in the query, then results include the student with the teacher with no skills
					students = session.createQuery(
							"select student from Student student where size(student.teacher.skills) > -1",
							Student.class
					).getResultList();
					assertEquals( 3, students.size() );
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
									"	left join fetch t.skills " +
									"where size(student.teacher.skills) > 2",
							Student.class
					).getResultList();
					assertEquals( 0, students.size() );
					statementInspector.assertNumberOfJoins( 0, 3 );

					students = session.createQuery(
							"select distinct student from Student student left join fetch student.teacher t left join fetch t.skills where size(student.teacher.skills) > 1",
							Student.class
					).getResultList();
					assertEquals( 1, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );

					students = session.createQuery(
							"select distinct student from Student student left join fetch student.teacher t left join fetch t.skills where size(student.teacher.skills) > 0",
							Student.class
					).getResultList();
					assertEquals( 2, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );
					assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getSkills() ) );

					students = session.createQuery(
							"select distinct student from Student student left join fetch student.teacher t left join fetch t.skills where size(student.teacher.skills) > -1",
							Student.class
					).getResultList();
					assertEquals( 3, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );
					assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getSkills() ) );
					assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getSkills() ) );
				}
		);
	}

	@Test
	public void testSizeWithNestedPath(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					{
						List<Student> students = session.createQuery(
								"select student from Student student join student.teacher t join t.skills",
								Student.class
						).getResultList();

					}

					final SQLStatementInspector sqlStatementInspector = extractFromSession( session );
					sqlStatementInspector.clear();
					List<Student> students = session.createQuery(
							"select student from Student student join student.teacher t where size(student.teacher.skills) > -1",
							Student.class
					).getResultList();
					assertEquals( 3L, students.size() );
					sqlStatementInspector.assertNumberOfJoins( 0, 1 );

					students = session.createQuery(
							"select student from Student student join student.teacher t where size(student.teacher.skills) > 0",
							Student.class
					).getResultList();
					assertEquals( 2L, students.size() );

					students = session.createQuery(
							"select student from Student student join student.teacher t where size(student.teacher.skills) > 1",
							Student.class
					).getResultList();
					assertEquals( 1L, students.size() );

					students = session.createQuery(
							"select student from Student student join student.teacher t where size(student.teacher.skills) > 2",
							Student.class
					).getResultList();
					assertEquals( 0L, students.size() );
				}
		);
	}

	@Test
	public void testSizeWithNestedPathAddFetchDistinct(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final SQLStatementInspector sqlStatementInspector = extractFromSession( session );
					sqlStatementInspector.clear();
					List<Student> students = session.createQuery(
							"select distinct student from Student student join student.teacher t join fetch student.teacher tjoin left join fetch tjoin.skills where size(student.teacher.skills) > -1",
							Student.class
					).getResultList();
					sqlStatementInspector.assertNumberOfJoins( 0, 3 );
					assertEquals( 3L, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );
					assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getSkills() ) );
					assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getSkills() ) );

					students = session.createQuery(
							"select distinct student from Student student join student.teacher t join fetch student.teacher tjoin left join fetch tjoin.skills where size(student.teacher.skills) > 0",
							Student.class
					).getResultList();
					assertEquals( 2L, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );
					assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getSkills() ) );

					students = session.createQuery(
							"select distinct student from Student student join student.teacher t join fetch student.teacher tjoin left join fetch tjoin.skills where size(student.teacher.skills) > 1",
							Student.class
					).getResultList();
					assertEquals( 1L, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );

					students = session.createQuery(
							"select distinct student from Student student join student.teacher t join fetch student.teacher tjoin left join fetch tjoin.skills where size(student.teacher.skills) > 2",
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
					final SQLStatementInspector sqlStatementInspector = extractFromSession( session );
					sqlStatementInspector.clear();

					List<Student> students = session.createQuery(
							"select student from Student student join student.teacher t where size(t.skills) > -1",
							Student.class
					).getResultList();
					assertEquals( 3, students.size() );
					sqlStatementInspector.assertNumberOfJoins( 0, 1 );

					students = session.createQuery(
							"select student from Student student join student.teacher t where size(t.skills) > 0",
							Student.class
					).getResultList();
					assertEquals( 2, students.size() );

					students = session.createQuery(
							"select student from Student student join student.teacher t where size(t.skills) > 1",
							Student.class
					).getResultList();
					assertEquals( 1, students.size() );

					students = session.createQuery(
							"select student from Student student join student.teacher t where size(t.skills) > 2",
							Student.class
					).getResultList();
					assertEquals( 0, students.size() );
				}
		);
	}

	@Test
	public void testSizeWithNestedPath2AddFetchDistinct(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final SQLStatementInspector sqlStatementInspector = extractFromSession( session );
					sqlStatementInspector.clear();

					List<Student> students = session.createQuery(
							"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch left join fetch tfetch.skills where size(t.skills) > -1",
							Student.class
					).getResultList();
					// NOTE: An INNER JOIN is done on Teacher twice, which results in 4 joins.
					//       A possible optimization would be to reuse this INNER JOIN.
					sqlStatementInspector.assertNumberOfJoins( 0, 4 );
					assertEquals( 3L, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );
					assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getSkills() ) );
					assertTrue( Hibernate.isInitialized( students.get( 2 ).getTeacher().getSkills() ) );

					students = session.createQuery(
							"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch left join fetch tfetch.skills where size(t.skills) > 0",
							Student.class
					).getResultList();
					assertEquals( 2L, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );
					assertTrue( Hibernate.isInitialized( students.get( 1 ).getTeacher().getSkills() ) );

					students = session.createQuery(
							"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch left join fetch tfetch.skills where size(t.skills) > 1",
							Student.class
					).getResultList();
					assertEquals( 1L, students.size() );
					assertTrue( Hibernate.isInitialized( students.get( 0 ).getTeacher().getSkills() ) );

					students = session.createQuery(
							"select distinct student from Student student join student.teacher t join fetch student.teacher tfetch left join fetch tfetch.skills where size(t.skills) > 2",
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
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

}
