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
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.collection.spi.PersistentSet;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class FetchTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyStatisticsSupport( true );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( School.class );
		sources.addAnnotatedClass( Primary.class );
		sources.addAnnotatedClass( Secondary.class );
		sources.addAnnotatedClass( Student.class );
	}

	@Test
	public void testFetch() {
		inStatelessTransaction(
				session -> {
					Secondary secondary = new Secondary( "BHS" );
					Student student = new Student( "gavin" );
					student.school = secondary;
					session.insert(secondary);
					session.insert(student);
				}
		);

		inStatelessSession(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();
					final Student student = session.get( Student.class, "gavin" );
					assertFalse( Hibernate.isInitialized( student.school) );
					assertTrue( student.school instanceof HibernateProxy );
					long count = stats.getPrepareStatementCount();
					session.fetch( student.school);
					assertTrue( Hibernate.isInitialized( student.school) );
					assertEquals( "BHS", student.school.getName() );

					assertEquals( count+1, stats.getPrepareStatementCount() );
				}
		);

		inStatelessSession(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();
					final School school = session.get( School.class, "BHS" );
					assertFalse( Hibernate.isInitialized( school.students) );
					assertTrue( school.students instanceof PersistentSet );
					long count = stats.getPrepareStatementCount();
					session.fetch( school.students);
					assertTrue( Hibernate.isInitialized( school.students) );
					assertEquals( 1, school.students.size() );

					assertEquals( count+1, stats.getPrepareStatementCount() );
				}
		);
	}

	@Test
	public void testFetchEmpty() {
		inStatelessTransaction(
				session -> {
					Secondary secondary = new Secondary( "BHS" );
					session.insert(secondary);
				}
		);

		inStatelessSession(
				session -> {
					final Statistics stats = sessionFactory().getStatistics();
					stats.clear();
					final School school = session.get( School.class, "BHS" );
					assertFalse( Hibernate.isInitialized( school.students) );
					assertTrue( school.students instanceof PersistentSet );
					long count = stats.getPrepareStatementCount();
					session.fetch( school.students);
					assertTrue( Hibernate.isInitialized( school.students) );
					assertTrue( school.students.isEmpty() );

					assertEquals( count+1, stats.getPrepareStatementCount() );
				}
		);
	}

	@After
	public void cleanUpTestData() {
		inTransaction(
				session -> {
					session.createQuery( "delete from SchoolStudent" ).executeUpdate();
					session.createQuery( "delete from School" ).executeUpdate();
				}
		);
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

		protected Secondary() {}

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

		protected Student() {}

		public Student(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}
	}
}
