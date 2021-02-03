/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance;

import java.util.Comparator;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@TestForIssue(jiraKey = "HHH-14103")
public class TransientOverrideAsPersistentJoined extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testFindByRootClass() {
		doInHibernate( this::sessionFactory, session -> {
			final Employee editor = session.find( Employee.class, "Jane Smith" );
			assertNotNull( editor );
			assertEquals( "Senior Editor", editor.getTitle() );
			final Employee writer = session.find( Employee.class, "John Smith" );
			assertTrue( Writer.class.isInstance( writer ) );
			assertEquals( "Writing", writer.getTitle() );
			assertNotNull( ( (Writer) writer ).getGroup() );
			final Group group = ( (Writer) writer ).getGroup();
			assertEquals( writer.getTitle(), group.getName() );
			final Job jobEditor = session.find( Job.class, "Edit" );
			assertSame( editor, jobEditor.getEmployee() );
			final Job jobWriter = session.find( Job.class, "Write" );
			assertSame( writer, jobWriter.getEmployee() );
		});
	}

	@Test
	public void testFindBySubclass() {
		doInHibernate( this::sessionFactory, session -> {
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
		});
	}

	@Test
	public void testQueryByRootClass() {
		doInHibernate( this::sessionFactory, session -> {
			final List<Employee> employees = session.createQuery( "from Employee", Employee.class )
					.getResultList();
			assertEquals( 2, employees.size() );
			employees.sort( Comparator.comparing( Employee::getName ) );
			assertTrue( Editor.class.isInstance( employees.get( 0 ) ) );
			assertTrue( Writer.class.isInstance( employees.get( 1 ) ) );
			final Editor editor = (Editor) employees.get( 0 );
			assertEquals( "Senior Editor", editor.getTitle() );
			final Writer writer = (Writer) employees.get( 1 );
			assertEquals( "Writing", writer.getTitle() );
			assertNotNull( writer.getGroup() );
			final Group group = writer.getGroup();
			assertEquals( writer.getTitle(), group.getName() );
		});
	}

	@Test
	@FailureExpected( jiraKey = "HHH-12981")
	public void testQueryByRootClassAndOverridenProperty() {
		doInHibernate( this::sessionFactory, session -> {
			final Employee editor = session.createQuery( "from Employee where title=:title", Employee.class )
					.setParameter( "title", "Senior Editor" )
					.getSingleResult();
			assertTrue( Editor.class.isInstance( editor ) );

			final Employee writer = session.createQuery( "from Employee where title=:title", Employee.class )
					.setParameter( "title", "Writing" )
					.getSingleResult();
			assertTrue( Writer.class.isInstance( writer ) );
			assertNotNull( ( (Writer) writer ).getGroup() );
			assertEquals( writer.getTitle(), ( (Writer) writer ).getGroup() .getName() );
		});
	}

	@Test
	@FailureExpected( jiraKey = "HHH-12981")
	public void testQueryByRootClassAndOverridenPropertyTreat() {
		doInHibernate( this::sessionFactory, session -> {
			final Employee editor = session.createQuery( "from Employee e where treat( e as Editor ).title=:title", Employee.class )
					.setParameter( "title", "Senior Editor" )
					.getSingleResult();
			assertTrue( Editor.class.isInstance( editor ) );

			final Employee writer = session.createQuery( "from Employee e where treat( e as Writer).title=:title", Employee.class )
					.setParameter( "title", "Writing" )
					.getSingleResult();
			assertTrue( Writer.class.isInstance( writer ) );
			assertNotNull( ( (Writer) writer ).getGroup() );
			assertEquals( writer.getTitle(), ( (Writer) writer ).getGroup() .getName() );
		});
}

	@Test
	public void testQueryBySublassAndOverridenProperty() {
		doInHibernate( this::sessionFactory, session -> {
			final Editor editor = session.createQuery( "from Editor where title=:title", Editor.class )
					.setParameter( "title", "Senior Editor" )
					.getSingleResult();
			assertTrue( Editor.class.isInstance( editor ) );

			final Writer writer = session.createQuery( "from Writer where title=:title", Writer.class )
					.setParameter( "title", "Writing" )
					.getSingleResult();
			assertNotNull( writer.getGroup() );
			assertEquals( writer.getTitle(), writer.getGroup().getName() );
		});
	}

	@Test
	@FailureExpected( jiraKey = "HHH-12981")
	public void testCriteriaQueryByRootClassAndOverridenProperty() {
		doInHibernate( this::sessionFactory, session -> {

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
			assertTrue( Editor.class.isInstance( editor ) );

			final Predicate predicateWriter = builder.equal(
					builder.treat( root, Writer.class ).get( "title" ),
					parameter
			);
			query.where( predicateWriter );
			final Employee writer = session.createQuery( query )
					.setParameter( "title", "Writing" )
					.getSingleResult();
			assertTrue( Writer.class.isInstance( writer ) );
			assertNotNull( ( (Writer) writer ).getGroup() );
			assertEquals( writer.getTitle(), ( (Writer) writer ).getGroup() .getName() );
		});
	}

	@Before
	public void setupData() {

		doInHibernate( this::sessionFactory, session -> {
			Job jobEditor = new Job("Edit");
			jobEditor.setEmployee(new Editor("Jane Smith", "Senior Editor"));
			Job jobWriter= new Job("Write");
			jobWriter.setEmployee(new Writer("John Smith", new Group("Writing")));

			Employee editor = jobEditor.getEmployee();
			Employee writer = jobWriter.getEmployee();
			Group group = Writer.class.cast( writer ).getGroup();

			session.persist( editor );
			session.persist( group );
			session.persist( writer );
			session.persist( jobEditor );
			session.persist( jobWriter );
		});
	}

	@After
	public void cleanupData() {
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "delete from Job" ).executeUpdate();
			session.createQuery( "delete from Employee" ).executeUpdate();
			session.createQuery( "delete from Group" ).executeUpdate();
		});
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Employee.class,
				Editor.class,
				Writer.class,
				Group.class,
				Job.class
		};
	}

	@Entity(name="Employee")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(name="department")
	public static abstract class Employee {
		private String name;
		private String title;

		protected Employee(String name) {
			this();
			setName(name);
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
			super(name);
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
			super(name);
			setGroup(group);
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

		public Group(String name) {
			this();
			setName(name);
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

		public Job(String name) {
			this();
			setName(name);
		}

		@Id
		public String getName() {
			return name;
		}

		@OneToOne
		@JoinColumn(name="employee_name")
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
