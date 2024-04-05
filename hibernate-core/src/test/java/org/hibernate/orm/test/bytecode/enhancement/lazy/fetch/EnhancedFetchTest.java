/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.fetch;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.collection.spi.PersistentSet;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@DomainModel(
		annotatedClasses = {
				EnhancedFetchTest.School.class, EnhancedFetchTest.Student.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_QUERY_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
public class EnhancedFetchTest {

	@Test
	public void testFetch(SessionFactoryScope scope) {
		scope.inStatelessTransaction(
				session -> {
					School school = new School( "BHS" );
					Student student = new Student( "gavin" );
					student.school = school;
					session.insert(school);
					session.insert(student);
				}
		);

		scope.inStatelessSession(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();
					final Student student = session.get( Student.class, "gavin" );
					assertFalse( Hibernate.isInitialized( student.getSchool() ) );
					assertFalse( student.getSchool() instanceof HibernateProxy );
					assertTrue( student.getSchool() instanceof PersistentAttributeInterceptable );
					long count = stats.getPrepareStatementCount();
					session.fetch( student.getSchool() );
					assertTrue( Hibernate.isInitialized( student.getSchool() ) );
					assertEquals( "BHS", student.getSchool().getName() );

					assertEquals( count+1, stats.getPrepareStatementCount() );
				}
		);

		scope.inStatelessSession(
				session -> {
					final Statistics stats = scope.getSessionFactory().getStatistics();
					stats.clear();
					final School school = session.get( School.class, "BHS" );
					assertFalse( Hibernate.isInitialized( school.getStudents() ) );
					assertTrue( school.getStudents() instanceof PersistentSet );
					long count = stats.getPrepareStatementCount();
					session.fetch( school.getStudents() );
					assertTrue( Hibernate.isInitialized( school.getStudents() ) );

					assertEquals( count+1, stats.getPrepareStatementCount() );
				}
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Pupil" ).executeUpdate();
					session.createQuery( "delete from School" ).executeUpdate();
				}
		);
	}

	@Entity(name = "School")
	public static class School {

		@Id
		private String name;

		private int age;

		public School(String name) {
			this.name = name;
		}

		School() {}

		@OneToMany(mappedBy = "school")
		private Set<Student> students;

		public String getName() {
			return name;
		}

		protected void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public Set<Student> getStudents() {
			return students;
		}
	}

	@Entity(name = "Pupil")
	public static class Student {

		@Id
		private String id;

		@ManyToOne(fetch = FetchType.LAZY)
		private School school = null;

		protected Student() {}

		public Student(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public School getSchool() {
			return school;
		}
	}
}
