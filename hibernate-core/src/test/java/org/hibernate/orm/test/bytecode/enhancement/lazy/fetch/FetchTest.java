/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.fetch;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.Hibernate;
import org.hibernate.collection.spi.PersistentSet;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@DomainModel(
		annotatedClasses = {
				FetchTest.School.class,
				FetchTest.Primary.class,
				FetchTest.Secondary.class,
				FetchTest.Student.class
		}
)
@SessionFactory(generateStatistics = true)
public class FetchTest {

	@Test
	public void testFetch(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
					Secondary secondary = new Secondary( "BHS" );
					Student student = new Student( "gavin" );
					student.school = secondary;
					session.insert( secondary );
					session.insert( student );
				}
		);

		scope.inStatelessTransaction( session -> {
					final Statistics stats = session.getFactory().getStatistics();
					stats.clear();
					final Student student = session.get( Student.class, "gavin" );
					assertFalse( Hibernate.isInitialized( student.school ) );
					assertInstanceOf( HibernateProxy.class, student.school );
					long count = stats.getPrepareStatementCount();
					session.fetch( student.school );
					assertTrue( Hibernate.isInitialized( student.school ) );
					assertEquals( "BHS", student.school.getName() );

					assertEquals( count + 1, stats.getPrepareStatementCount() );
				}
		);

		scope.inStatelessTransaction( session -> {
					final Statistics stats = session.getFactory().getStatistics();
					stats.clear();
					final School school = session.get( School.class, "BHS" );
					assertFalse( Hibernate.isInitialized( school.students ) );
					assertInstanceOf( PersistentSet.class, school.students );
					long count = stats.getPrepareStatementCount();
					session.fetch( school.students );
					assertTrue( Hibernate.isInitialized( school.students ) );
					assertEquals( 1, school.students.size() );

					assertEquals( count + 1, stats.getPrepareStatementCount() );
				}
		);
	}

	@Test
	public void testFetchEmpty(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
					Secondary secondary = new Secondary( "BHS" );
					session.insert( secondary );
				}
		);

		scope.inStatelessTransaction( session -> {
					final Statistics stats = session.getFactory().getStatistics();
					stats.clear();
					final School school = session.get( School.class, "BHS" );
					assertFalse( Hibernate.isInitialized( school.students ) );
					assertInstanceOf( PersistentSet.class, school.students );
					long count = stats.getPrepareStatementCount();
					session.fetch( school.students );
					assertTrue( Hibernate.isInitialized( school.students ) );
					assertTrue( school.students.isEmpty() );

					assertEquals( count + 1, stats.getPrepareStatementCount() );
				}
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "School")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static abstract class School {

		@Id
		private String name;

		private int age;

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
	}

	@Entity(name = "PrimarySchool")
	public static class Primary extends School {

		public Primary(String name) {
			this();
			setName( name );
		}

		protected Primary() {
			// this form used by Hibernate
		}
	}

	@Entity(name = "SecondarySchool")
	public static class Secondary extends Primary {

		private String sex;

		public Secondary(String name) {
			this();
			setName( name );
		}

		protected Secondary() {
		}

		public String getSex() {
			return sex;
		}

		public void setSex(String sex) {
			this.sex = sex;
		}
	}

	@Entity(name = "SchoolStudent")
	public static class Student {

		@Id
		private String id;

		@ManyToOne(fetch = FetchType.LAZY)
		private School school = null;

		protected Student() {
		}

		public Student(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}
}
