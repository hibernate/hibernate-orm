/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-9865")
public class ImplicitCompositeKeyJoinTest {
	private static final Logger LOGGER = Logger.getLogger( ImplicitCompositeKeyJoinTest.class );

	private final static String EXPECTED_SQL = "create table Employee " +
			"(age varchar(15) not null" +
			", birthday varchar(255) not null" +
			", name varchar(20) not null" +
			", manager_age varchar(15)" +
			", manager_birthday varchar(255)" +
			", manager_name varchar(20)" +
			", primary key (age, birthday, name))";

	@Test
	public void testImplicitCompositeJoin() throws Exception {

		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
		try {
			org.hibernate.boot.Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( Employee.class )
					.buildMetadata();

			boolean passed = false;

			List<String> commands = new SchemaCreatorImpl().generateCreationCommands(
					metadata,
					false
			);
			for ( String command : commands ) {
				LOGGER.info( command );

				if ( EXPECTED_SQL.equals( command ) ) {
					passed = true;
				}
			}
			assertTrue(
					"Expected create table command for Employee entity not found",
					passed
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity
	@Table(name = "Employee")
	public class Employee {
		@EmbeddedId
		@ForeignKey(name = "none")
		private EmployeeId id;

		@ManyToOne(optional = true)
		@ForeignKey(name = "none")
		private Employee manager;

		public void setId(EmployeeId id) {
			this.id = id;
		}

		public EmployeeId getId() {
			return id;
		}

		public void setManager(Employee manager) {
			this.manager = manager;
		}

		public Employee getManager() {
			return manager;
		}
	}

	@Embeddable
	public class EmployeeId implements Serializable {
		private static final long serialVersionUID = 1L;

		public EmployeeId(String name, String birthday, String age) {
			this.name = name;
			this.birthday = birthday;
			this.age = age;
		}

		@Column(length = 15)
		public String age;

		@Column(length = 20)
		private String name;

		private String birthday;

		@Override
		public int hashCode() {
			int hash = 1;
			hash = hash * 31 + (name != null ? name.hashCode() : 0);
			hash = hash * 31 + (age != null ? age.hashCode() : 0);
			return hash * 31 + (birthday != null ? birthday.hashCode() : 0);
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == this ) {
				return true;
			}

			if ( !(obj instanceof EmployeeId) ) {
				return false;
			}
			EmployeeId that = (EmployeeId) obj;
			if ( age != that.age ) {
				return false;
			}
			if ( birthday != that.birthday ) {
				return false;
			}
			if ( name != null && !name.equals( that.name ) ) {
				return false;
			}
			return true;
		}

		public void setAge(String age) {
			this.age = age;
		}

		public void setName(String name) {
			this.name = name;
		}


		public void setBirthday(String birthday) {
			this.birthday = birthday;
		}

		public String getAge() {
			return age;
		}

		public String getName() {
			return name;
		}

		public String getBirthday() {
			return birthday;
		}
	}
}
