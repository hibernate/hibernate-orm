package org.hibernate.orm.test.collection.idbag;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jan Schatteman
 */
@DomainModel(
		annotatedClasses = {PersistentIdBagTest2.Student.class, PersistentIdBagTest2.Course.class}
)
@SessionFactory
@Jira( value = "https://hibernate.atlassian.net/browse/HHH-10875")
public class PersistentIdBagTest2 {

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testIteratorRemove(SessionFactoryScope scope) {
		Assertions.assertDoesNotThrow( () ->
				scope.inTransaction(
						session -> {
							Student s = createParentWithThreeChildren();
							session.persist(s);
							session.flush();

							Iterator<?> it = s.getCourses().iterator();
							it.next();
							it.remove();
							session.flush();
						}
				)
		);
	}

	@Test
	public void testListIteratorNextRemove(SessionFactoryScope scope) {
		Assertions.assertDoesNotThrow( () ->
				scope.inTransaction(
						session -> {
							Student s = createParentWithThreeChildren();
							session.persist(s);
							session.flush();

							ListIterator<?> it = s.getCourses().listIterator();
							it.next();
							it.remove();
							session.flush();
						}
				)
		);
	}

	@Test
	public void testListIteratorPreviousRemove(SessionFactoryScope scope) {
		Assertions.assertDoesNotThrow( () ->
				scope.inTransaction(
						session -> {
							Student s = createParentWithThreeChildren();
							session.persist(s);
							session.flush();

							ListIterator<?> it = s.getCourses().listIterator();
							it.next();
							it.previous();
							it.remove();
							session.flush();
						}
				)
		);
	}

	@Test
	public void testSetElement(SessionFactoryScope scope) {
		// Test for the change in BasicCollectionDecomposer.bindUpdateRowRestrictions()
		Student student = scope.fromTransaction(
				session -> {
					Student s = createParentWithThreeChildren();
					session.persist(s);
					session.flush();

					Course replacement = new Course( "replacement" );
					session.persist( replacement );
					s.getCourses().set( 0, replacement );
					session.flush();
					return s;
				}
		);
		assertEquals("replacement", student.getCourses().get(0).getTitle());
	}

	private Student createParentWithThreeChildren() {
		Student parent = new Student( "root" );
		Course c1 = new Course( "c1" );
		parent.getCourses().add( c1 );
		Course c2 = new Course( "c2" );
		parent.getCourses().add( c2 );
		Course c3 = new Course( "c3" );
		parent.getCourses().add( c3 );
		return parent;
	}

	@Entity(name="Student")
	@Table(name = "students")
	@SequenceGenerator(name = "contact_id_seq", sequenceName = "contact_id_seq")
	public static class Student {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToMany(cascade = CascadeType.PERSIST)
		@JoinTable(
				name = "student_courses",
				joinColumns = @JoinColumn(name = "student_id"),
				inverseJoinColumns = @JoinColumn(name = "course_id")
		)
		@CollectionId(
				column = @Column(name = "id"),
				generator = "contact_id_seq"
		)
		@CollectionIdJdbcTypeCode(Types.BIGINT)
		private List<Course> courses = new ArrayList<>();

		public Student() {
		}

		public Student(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Course> getCourses() {
			return courses;
		}

		public void setCourses(List<Course> courses) {
			this.courses = courses;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name="Course")
	public static class Course {
		@Id
		@GeneratedValue
		private Long id;
		private String title;

		public Course() {
		}

		public Course(String title) {
			this.title = title;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}
}
