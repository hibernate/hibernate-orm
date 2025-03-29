/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance;

import java.util.Comparator;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.ExceptionHelper;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;


@JiraKey("HHH-14103")
@DomainModel(
		annotatedClasses = {
				TransientOverrideAsPersistentJoined.Employee.class,
				TransientOverrideAsPersistentJoined.Editor.class,
				TransientOverrideAsPersistentJoined.Writer.class,
				TransientOverrideAsPersistentJoined.Group.class,
				TransientOverrideAsPersistentJoined.Job.class
		}
)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.CRITERIA_COPY_TREE, value = "true"))
public class TransientOverrideAsPersistentJoined {

	@Test
	public void testFindByRootClass(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Employee editor = session.find( Employee.class, "Jane Smith" );
			assertNotNull( editor );
			assertEquals( "Senior Editor", editor.getTitle() );
			final Employee writer = session.find( Employee.class, "John Smith" );
			assertThat( writer, instanceOf( Writer.class ) );
			assertEquals( "Writing", writer.getTitle() );
			assertNotNull( ( (Writer) writer ).getGroup() );
			final Group group = ( (Writer) writer ).getGroup();
			assertEquals( writer.getTitle(), group.getName() );
			final Job jobEditor = session.find( Job.class, "Edit" );
			assertSame( editor, jobEditor.getEmployee() );
			final Job jobWriter = session.find( Job.class, "Write" );
			assertSame( writer, jobWriter.getEmployee() );
		} );
	}

	@Test
	public void testFindBySubclass(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Editor editor = session.find( Editor.class, "Jane Smith" );
			assertNotNull( editor );
			assertEquals( "Senior Editor", editor.getTitle() );
			final Writer writer = session.find( Writer.class, "John Smith" );
			assertEquals( "Writing", writer.getTitle() );
			assertNotNull( writer.getGroup() );
			final Group group = writer.getGroup();
			assertEquals( writer.getTitle(), group.getName() );
			final Job jobEditor = session.find( Job.class, "Edit" );
			assertSame( editor, jobEditor.getEmployee() );
			final Job jobWriter = session.find( Job.class, "Write" );
			assertSame( writer, jobWriter.getEmployee() );
		} );
	}

	@Test
	public void testQueryByRootClass(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Employee> employees = session.createQuery( "from Employee", Employee.class )
					.getResultList();
			assertEquals( 2, employees.size() );
			employees.sort( Comparator.comparing( Employee::getName ) );
			assertThat( employees.get( 0 ), instanceOf( Editor.class ) );
			assertThat( employees.get( 1 ), instanceOf( Writer.class ) );

			final Editor editor = (Editor) employees.get( 0 );
			assertEquals( "Senior Editor", editor.getTitle() );
			final Writer writer = (Writer) employees.get( 1 );
			assertEquals( "Writing", writer.getTitle() );
			assertNotNull( writer.getGroup() );
			final Group group = writer.getGroup();
			assertEquals( writer.getTitle(), group.getName() );
		} );
	}

	@Test
	@JiraKey("HHH-12981")
	public void testQueryByRootClassAndOverridenProperty(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				session.createQuery( "from Employee where title=:title", Employee.class );
				fail( "Expected exception!" );
			}
			catch (IllegalArgumentException e) {
				assertThat(
						ExceptionHelper.getRootCause( e ).getMessage(),
						containsString( "due to the attribute being declared in multiple subtypes" )
				);
			}
		} );
	}

	@Test
	@JiraKey("HHH-12981")
	public void testQueryByRootClassAndOverridenPropertyTreat(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Employee editor = session.createQuery(
					"from Employee e where treat( e as Editor ).title=:title",
					Employee.class
			)
					.setParameter( "title", "Senior Editor" )
					.getSingleResult();
			assertThat( editor, instanceOf( Editor.class ) );

			final Employee writer = session.createQuery(
					"from Employee e where treat( e as Writer).title=:title",
					Employee.class
			)
					.setParameter( "title", "Writing" )
					.getSingleResult();
			assertThat( writer, instanceOf( Writer.class ) );
			assertNotNull( ( (Writer) writer ).getGroup() );
			assertEquals( writer.getTitle(), ( (Writer) writer ).getGroup().getName() );
		} );
	}

	@Test
	public void testQueryBySublassAndOverridenProperty(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Editor editor = session.createQuery( "from Editor where title=:title", Editor.class )
					.setParameter( "title", "Senior Editor" )
					.getSingleResult();
			assertThat( editor, instanceOf( Editor.class ) );

			final Writer writer = session.createQuery( "from Writer where title=:title", Writer.class )
					.setParameter( "title", "Writing" )
					.getSingleResult();
			assertNotNull( writer.getGroup() );
			assertEquals( writer.getTitle(), writer.getGroup().getName() );
		} );
	}

	@Test
	@JiraKey("HHH-12981")
	public void testCriteriaQueryByRootClassAndOverridenProperty(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

			final CriteriaBuilder builder = session.getCriteriaBuilder();

			final CriteriaQuery<Employee> query = builder.createQuery( Employee.class );
			final Root<Employee> root = query.from( Employee.class );
			final ParameterExpression<String> parameter = builder.parameter( String.class, "title" );

			final Predicate predicateEditor = builder.equal(
					builder.treat( root, Editor.class ).get( "title" ),
					parameter
			);
			query.where( predicateEditor );
			final Employee editor = session.createQuery( query )
					.setParameter( "title", "Senior Editor" )
					.getSingleResult();
			assertThat( editor, instanceOf( Editor.class ) );

			final Predicate predicateWriter = builder.equal(
					builder.treat( root, Writer.class ).get( "title" ),
					parameter
			);
			query.where( predicateWriter );
			final Employee writer = session.createQuery( query )
					.setParameter( "title", "Writing" )
					.getSingleResult();
			assertThat( writer, instanceOf( Writer.class ) );
			assertNotNull( ( (Writer) writer ).getGroup() );
			assertEquals( writer.getTitle(), ( (Writer) writer ).getGroup().getName() );
		} );
	}

	@BeforeEach
	public void setupData(SessionFactoryScope scope) {

		scope.inTransaction( session -> {
			Job jobEditor = new Job( "Edit" );
			jobEditor.setEmployee( new Editor( "Jane Smith", "Senior Editor" ) );
			Job jobWriter = new Job( "Write" );
			jobWriter.setEmployee( new Writer( "John Smith", new Group( "Writing" ) ) );

			Employee editor = jobEditor.getEmployee();
			Employee writer = jobWriter.getEmployee();
			Group group = Writer.class.cast( writer ).getGroup();

			session.persist( editor );
			session.persist( group );
			session.persist( writer );
			session.persist( jobEditor );
			session.persist( jobWriter );
		} );
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "delete from Job" ).executeUpdate();
			session.createQuery( "delete from Employee" ).executeUpdate();
			session.createQuery( "delete from Group" ).executeUpdate();
		} );
	}

	@Entity(name = "Employee")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(name = "department")
	public static abstract class Employee {
		private String name;
		private String title;

		protected Employee(String name) {
			this();
			setName( name );
		}

		@Id
		public String getName() {
			return name;
		}

		@Transient
		public String getTitle() {
			return title;
		}

		protected Employee() {
			// this form used by Hibernate
		}

		protected void setName(String name) {
			this.name = name;
		}

		protected void setTitle(String title) {
			this.title = title;
		}
	}

	@Entity(name = "Editor")
	public static class Editor extends Employee {
		public Editor(String name, String title) {
			super( name );
			setTitle( title );
		}

		@Column(name = "e_title")
		public String getTitle() {
			return super.getTitle();
		}

		public void setTitle(String title) {
			super.setTitle( title );
		}

		protected Editor() {
			// this form used by Hibernate
			super();
		}
	}

	@Entity(name = "Writer")
	public static class Writer extends Employee {
		private Group group;

		public Writer(String name, Group group) {
			super( name );
			setGroup( group );
		}

		// Cannot have a constraint on e_title because
		// Editor#title (which uses the same e_title column) can be non-null,
		// and there is no associated group.
		@ManyToOne(optional = false)
		@JoinColumn(name = "e_title", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
		public Group getGroup() {
			return group;
		}

		@Column(name = "e_title", insertable = false, updatable = false)
		public String getTitle() {
			return super.getTitle();
		}

		public void setTitle(String title) {
			super.setTitle( title );
		}

		protected Writer() {
			// this form used by Hibernate
			super();
		}

		protected void setGroup(Group group) {
			this.group = group;
			setTitle( group.getName() );
		}
	}

	@Entity(name = "Group")
	@Table(name = "WorkGroup")
	public static class Group {
		private String name;

		private String details;

		public Group(String name) {
			this();
			setName( name );
		}

		@Id
		public String getName() {
			return name;
		}

		protected Group() {
			// this form used by Hibernate
		}

		protected void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Job")
	public static class Job {
		private String name;
		private Employee employee;
		private String description;

		public Job(String name) {
			this();
			setName( name );
		}

		@Id
		public String getName() {
			return name;
		}

		@OneToOne
		@JoinColumn(name = "employee_name")
		public Employee getEmployee() {
			return employee;
		}

		protected Job() {
			// this form used by Hibernate
		}

		protected void setName(String name) {
			this.name = name;
		}

		protected void setEmployee(Employee e) {
			this.employee = e;
		}
	}
}
