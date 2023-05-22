package org.hibernate.orm.test.cascade;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Transaction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				OneToManyMergeTest.Course.class,
				OneToManyMergeTest.Student.class,
				OneToManyMergeTest.Enrollment.class,
		}
)
@SessionFactory
public class OneToManyMergeTest {

	public static final String STUDENT_NAME = "John";
	public static final String COURSE_1_NAME = "ENGL";
	public static final String COURSE_2_NAME = "HISTORY";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Student student = new Student( STUDENT_NAME );
					session.persist( student );

					Course course1 = new Course( COURSE_1_NAME );
					session.persist( course1 );

					Course course2 = new Course( COURSE_2_NAME );
					session.persist( course2 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Enrollment" ).executeUpdate();
					session.createQuery( "delete from Course" ).executeUpdate();
					session.createQuery( "delete from Student " ).executeUpdate();
				}
		);
	}

	@Test
	@JiraKey("HHH-16627")
	public void testMergeManagedInstance(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Student s1 = session.createQuery( "from Student where name = :name", Student.class )
							.setParameter( "name", STUDENT_NAME ).uniqueResult();

					Course c1 = session.createQuery( "from Course where name = :name", Course.class )
							.setParameter( "name", COURSE_1_NAME ).uniqueResult();

					Enrollment e1 = new Enrollment( 10 );

					s1.addEnrollment( e1 );

					c1.addEnrollment( e1 );

					session.merge( s1 );
				}
		);

		scope.inTransaction(
				session -> {
					List<Enrollment> enrls = session.createQuery(
							"from Enrollment e where e.student.name = :name",
							Enrollment.class
					).setParameter( "name", STUDENT_NAME ).list();
					assertThat( enrls.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	@JiraKey("HHH-16627")
	public void testMergeManagedInstance2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Student s1 = session.createQuery( "from Student where name = :name", Student.class )
							.setParameter( "name", STUDENT_NAME ).uniqueResult();

					Course c1 = session.createQuery( "from Course where name = :name", Course.class )
							.setParameter( "name", COURSE_1_NAME ).uniqueResult();

					Enrollment e1 = new Enrollment( 10 );

					s1.addEnrollment( e1 );

					c1.addEnrollment( e1 );

					Student mergedStudent = session.merge( s1 );
					Course mergedCourse = session.merge( c1 );

					assertSameEnrollmentInstance( mergedStudent, mergedCourse );
				}
		);

		scope.inTransaction(
				session -> {
					List<Enrollment> enrls = session.createQuery(
							"from Enrollment e where e.student.name = :name",
							Enrollment.class
					).setParameter( "name", STUDENT_NAME ).list();
					assertThat( enrls.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	@JiraKey("HHH-16627")
	public void testMergeDetachedInstance(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {

					Student s1 = new Student( STUDENT_NAME );

					Course c1 = session.createQuery( "from Course where name = :name", Course.class )
							.setParameter( "name", COURSE_1_NAME ).uniqueResult();

					Enrollment e1 = new Enrollment( 10 );

					s1.addEnrollment( e1 );
					c1.addEnrollment( e1 );

					session.merge( s1 );
				}
		);

		scope.inTransaction(
				session -> {
					List<Enrollment> enrls = session.createQuery(
							"from Enrollment e where e.student.name = :name",
							Enrollment.class
					).setParameter( "name", STUDENT_NAME ).list();
					assertThat( enrls.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	@JiraKey("HHH-16627")
	public void testMergeDetachedInstance2(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {

					Student s1 = new Student( STUDENT_NAME );

					Course c1 = session.createQuery( "from Course where name = :name", Course.class )
							.setParameter( "name", COURSE_1_NAME ).uniqueResult();

					Enrollment e1 = new Enrollment( 10 );

					s1.addEnrollment( e1 );
					c1.addEnrollment( e1 );

					Student mergedStudent = session.merge( s1 );
					Course mergedCourse = session.merge( c1 );

					assertSameEnrollmentInstance( mergedStudent, mergedCourse );
				}
		);

		scope.inTransaction(
				session -> {
					List<Enrollment> enrls = session.createQuery(
							"from Enrollment e where e.student.name = :name",
							Enrollment.class
					).setParameter( "name", STUDENT_NAME ).list();
					assertThat( enrls.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	@JiraKey("HHH-16627")
	public void testMergeTransientInstance(SessionFactoryScope scope) {

		final String newStudentName = "Bill";
		scope.inTransaction(
				session -> {

					Student s1 = new Student( newStudentName );
					session.persist( s1 );

					Course c1 = session.createQuery( "from Course where name = :name", Course.class )
							.setParameter( "name", COURSE_1_NAME ).uniqueResult();

					Enrollment e1 = new Enrollment( 10 );

					s1.addEnrollment( e1 );
					c1.addEnrollment( e1 );

					session.merge( s1 );
				}
		);

		scope.inTransaction(
				session -> {
					List<Enrollment> enrls = session.createQuery(
							"from Enrollment e where e.student.name = :name",
							Enrollment.class
					).setParameter( "name", newStudentName ).list();
					assertThat( enrls.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	@JiraKey("HHH-16627")
	public void testMergeTransientInstance2(SessionFactoryScope scope) {

		final String newStudentName = "Bill";
		scope.inTransaction(
				session -> {

					Student s1 = new Student( newStudentName );
					session.persist( s1 );

					Course c1 = session.createQuery( "from Course where name = :name", Course.class )
							.setParameter( "name", COURSE_1_NAME ).uniqueResult();

					Enrollment e1 = new Enrollment( 10 );

					s1.addEnrollment( e1 );
					c1.addEnrollment( e1 );

					Student mergedStudent = session.merge( s1 );
					Course mergedCourse = session.merge( c1 );

					assertSameEnrollmentInstance( mergedStudent, mergedCourse );
				}
		);

		scope.inTransaction(
				session -> {
					List<Enrollment> enrls = session.createQuery(
							"from Enrollment e where e.student.name = :name",
							Enrollment.class
					).setParameter( "name", newStudentName ).list();
					assertThat( enrls.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	@JiraKey("HHH-16628")
	public void twoTransactionsTestMerge(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Transaction t1 = session.beginTransaction();
					try {
						Student s1 = session.createQuery( "from Student where name = :name", Student.class )
								.setParameter( "name", STUDENT_NAME )
								.uniqueResult();
						Course c1 = session.createQuery( "from Course where name = :name", Course.class )
								.setParameter( "name", COURSE_1_NAME ).uniqueResult();
						Course c2 = session.createQuery( "from Course where name = :name", Course.class )
								.setParameter( "name", COURSE_2_NAME ).uniqueResult();

						Enrollment e1 = new Enrollment();
						e1.setScore( 10 );
						e1.setStudent( s1 );
						s1.addEnrollment( e1 );
						e1.setCourse( c1 );
						c1.addEnrollment( e1 );

						Enrollment e2 = new Enrollment();
						e2.setScore( 20 );
						e2.setStudent( s1 );
						s1.addEnrollment( e2 );
						e2.setCourse( c2 );
						c2.addEnrollment( e2 );

						session.merge( s1 );
						t1.commit();

						t1 = session.beginTransaction();
						Course course = session.createQuery( "from Course where name = :name", Course.class )
								.setParameter( "name", COURSE_1_NAME ).uniqueResult();
						course.setCredit( 3 );

						session.merge( course );
						t1.commit();
					}
					finally {
						if ( t1.isActive() ) {
							t1.rollback();
						}
					}
				}
		);
	}

	@Test
	@JiraKey("HHH-16628")
	public void twoTransactionsTestPersistEach(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Transaction t1 = session.beginTransaction();
					try {
						Student s1 = session.createQuery( "from Student where name = :name", Student.class )
								.setParameter( "name", STUDENT_NAME ).uniqueResult();
						Course c1 = session.createQuery( "from Course where name = :name", Course.class )
								.setParameter( "name", COURSE_1_NAME ).uniqueResult();
						Course c2 = session.createQuery( "from Course where name = :name", Course.class )
								.setParameter( "name", COURSE_2_NAME ).uniqueResult();

						Enrollment e1 = new Enrollment();
						e1.setScore( 10 );
						e1.setStudent( s1 );
						s1.addEnrollment( e1 );
						e1.setCourse( c1 );
						c1.addEnrollment( e1 );
						session.persist( e1 );

						Enrollment e2 = new Enrollment();
						e2.setScore( 20 );
						e2.setStudent( s1 );
						s1.addEnrollment( e2 );
						e2.setCourse( c2 );
						c2.addEnrollment( e2 );
						session.persist( e2 );

						session.merge( s1 );
						t1.commit();

						t1 = session.beginTransaction();
						Course course = session.createQuery( "from Course where name = :name", Course.class )
								.setParameter( "name", COURSE_1_NAME ).uniqueResult();
						course.setCredit( 3 );

						session.merge( course );
						t1.commit();
					}
					finally {
						if ( t1.isActive() ) {
							t1.rollback();
						}
					}
				}
		);
	}

	@Test
	@JiraKey("HHH-16628")
	public void twoTransactionsTestCreate(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Transaction t1 = session.beginTransaction();
					try {
						Student s1 = new Student( "Betty" );
						Course c1 = session.createQuery( "from Course where name = :name", Course.class )
								.setParameter( "name", COURSE_1_NAME ).uniqueResult();
						Course c2 = session.createQuery( "from Course where name = :name", Course.class )
								.setParameter( "name", COURSE_2_NAME ).uniqueResult();

						Enrollment e1 = new Enrollment();
						e1.setScore( 10 );
						e1.setStudent( s1 );
						s1.addEnrollment( e1 );
						e1.setCourse( c1 );
						c1.addEnrollment( e1 );

						Enrollment e2 = new Enrollment();
						e2.setScore( 20 );
						e2.setStudent( s1 );
						s1.addEnrollment( e2 );
						e2.setCourse( c2 );
						c2.addEnrollment( e2 );

						session.persist( s1 );
						t1.commit();

						t1 = session.beginTransaction();
						Course course = session.createQuery( "from Course where name = :name", Course.class )
								.setParameter( "name", COURSE_1_NAME ).uniqueResult();
						course.setCredit( 3 );

						session.merge( course );
						t1.commit();
					}
					finally {
						if ( t1.isActive() ) {
							t1.rollback();
						}
					}
				}
		);
	}

	@Test
	@JiraKey("HHH-16628")
	public void twoTransactionsTestNewSession(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Student s1 = session.createQuery( "from Student where name = :name", Student.class )
							.setParameter( "name", STUDENT_NAME ).uniqueResult();
					Course c1 = session.createQuery( "from Course where name = :name", Course.class )
							.setParameter( "name", COURSE_1_NAME ).uniqueResult();
					Course c2 = session.createQuery( "from Course where name = :name", Course.class )
							.setParameter( "name", COURSE_2_NAME ).uniqueResult();

					Enrollment e1 = new Enrollment();
					e1.setScore( 10 );
					e1.setStudent( s1 );
					s1.addEnrollment( e1 );
					e1.setCourse( c1 );
					c1.addEnrollment( e1 );

					Enrollment e2 = new Enrollment();
					e2.setScore( 20 );
					e2.setStudent( s1 );
					s1.addEnrollment( e2 );
					e2.setCourse( c2 );
					c2.addEnrollment( e2 );

					session.merge( s1 );
				}
		);

		scope.inTransaction(
				session -> {
					Course course = session.createQuery( "from Course where name = :name", Course.class )
							.setParameter( "name", COURSE_1_NAME ).uniqueResult();
					course.setCredit( 3 );

					session.merge( course );
				}
		);
	}

	private static void assertSameEnrollmentInstance(Student mergedStudent, Course mergedCourse) {
		Set<Enrollment> mergedCourseEnrollments = mergedCourse.getEnrollments();
		assertThat( mergedCourseEnrollments.size() ).isEqualTo( 1 );

		Set<Enrollment> mergedStudentEnrollments = mergedStudent.getEnrollments();
		assertThat( mergedStudentEnrollments.size() ).isEqualTo( 1 );

		Enrollment courseEnrollment = mergedCourseEnrollments.iterator().next();
		Enrollment studentEnrollment = mergedStudentEnrollments.iterator().next();
		assertThat( courseEnrollment ).isEqualTo( studentEnrollment );
	}

	@Entity(name = "Student")
	@Table(name = "STUDENT")
	public static class Student {
		private Integer id;
		private String name;
		private Set<Enrollment> enrollments;

		public Student() {
		}

		public Student(String name) {
			this.name = name;
		}

		@Id
		@GeneratedValue
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

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "student", cascade = { CascadeType.ALL })
		public Set<Enrollment> getEnrollments() {
			return enrollments;
		}

		public void setEnrollments(Set<Enrollment> enrollments) {
			this.enrollments = enrollments;
		}

		public void addEnrollment(Enrollment enrollment) {
			if ( enrollments == null ) {
				enrollments = new HashSet<>();
			}
			enrollments.add( enrollment );
			enrollment.setStudent( this );
		}
	}

	@Entity(name = "Course")
	@Table(name = "COURSE")
	public static class Course {
		private Integer id;
		private String name;
		private Integer credit;
		private Set<Enrollment> enrollments;

		public Course() {
		}

		public Course(String name) {
			this.name = name;
		}

		@Id
		@GeneratedValue
		@Column(name = "id")
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

		public Integer getCredit() {
			return credit;
		}

		public void setCredit(Integer credit) {
			this.credit = credit;
		}

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "course", cascade = { CascadeType.ALL })
		public Set<Enrollment> getEnrollments() {
			return enrollments;
		}

		public void setEnrollments(Set<Enrollment> enrollments) {
			this.enrollments = enrollments;
		}

		public void addEnrollment(Enrollment enrollment) {
			enrollments.add( enrollment );
			enrollment.setCourse( this );
		}
	}

	@Entity(name = "Enrollment")
	@Table(name = "ENROLLMENT")
	public static class Enrollment {
		private Integer id;
		private Integer score;
		private Student student;
		private Course course;

		public Enrollment() {
		}

		public Enrollment(Integer score) {
			this.score = score;
		}

		@Id
		@GeneratedValue
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getScore() {
			return score;
		}

		public void setScore(Integer score) {
			this.score = score;
		}

		@ManyToOne(optional = false)
		@JoinColumn(name = "student_id")
		public Student getStudent() {
			return student;
		}

		public void setStudent(Student student) {
			this.student = student;
		}

		@ManyToOne(optional = false)
		@JoinColumn(name = "course_id")
		public Course getCourse() {
			return course;
		}

		public void setCourse(Course course) {
			this.course = course;
		}
	}
}
