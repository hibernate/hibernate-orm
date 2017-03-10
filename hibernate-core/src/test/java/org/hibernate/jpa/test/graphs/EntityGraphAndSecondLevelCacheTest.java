/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
public class EntityGraphAndSecondLevelCacheTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Student.class, Course.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		options.put( AvailableSettings.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		options.put( AvailableSettings.CACHE_REGION_PREFIX, "hibernate.test" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11385")
	public void hhh123Test() throws Exception {

		final Student student = new Student();
		final Set<Course> courses = TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			Course one = new Course();
			one.setName( "One" );
			Course two = new Course();
			two.setName( "two" );
			entityManager.persist( one );
			entityManager.persist( two );
			student.addCourse( one );
			student.addCourse( two );
			entityManager.persist( student );
			return student.getCourses();
		} );

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			EntityGraph<?> graph = entityManager.getEntityGraph( "Student.Course" );
			Map<String, Object> props = new HashMap<>();
			props.put( "javax.persistence.fetchgraph", graph );
			Student loadedStudent = entityManager.find( Student.class, student.getId(), props );
			loadedStudent.getCourses().size();
		} );

		final Student loadedStudent2 = TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			EntityGraph<?> graph = entityManager.getEntityGraph( "Student.Course" );
			Map<String, Object> props = new HashMap<>();
			props.put( "javax.persistence.fetchgraph", graph );
			return entityManager.find( Student.class, student.getId(), props );
		} );

		assertTrue( loadedStudent2.getCourses().containsAll( courses ) );
	}

	@Entity
	@Table(name = "COURSE")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Course {
		@Id
		@GeneratedValue
		private int id;

		private String name;

		@ManyToMany(mappedBy = "courses")
		private List<Student> students = new ArrayList<>();

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Student> getStudents() {
			return students;
		}

		public void setStudents(List<Student> students) {
			this.students = students;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getId();
			result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			int id = getId();
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			Course other = (Course) obj;
			if ( id != other.id ) {
				return false;
			}
			if ( name == null ) {
				if ( other.name != null ) {
					return false;
				}
			}
			else if ( !name.equals( other.name ) ) {
				return false;
			}
			return true;
		}
	}

	@Entity
	@Table(name = "STUDENT")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@NamedEntityGraph(
			name = "Student.Course",
			attributeNodes = {
					@NamedAttributeNode("courses")
			}
	)
	public static class Student {
		@Id
		@GeneratedValue
		private int id;

		@ManyToMany
		@JoinTable(
				name = "STUDENT_COURSES",
				joinColumns = @JoinColumn(referencedColumnName = "ID", name = "STUDENT_ID"),
				inverseJoinColumns = @JoinColumn(referencedColumnName = "ID", name = "COURSE_ID"),
				uniqueConstraints = {@UniqueConstraint(columnNames = {"STUDENT_ID", "COURSE_ID"})}
		)
		private Set<Course> courses = new HashSet<>();

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public Set<Course> getCourses() {
			return courses;
		}

		void setCourses(Set<Course> courses) {
			this.courses = courses;
		}

		public void addCourse(Course course) {
			course.getStudents().add( this );
			courses.add( course );
		}
	}
}
