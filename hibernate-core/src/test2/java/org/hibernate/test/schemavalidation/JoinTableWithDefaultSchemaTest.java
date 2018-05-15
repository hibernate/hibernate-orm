/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemavalidation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.TargetType;
import org.junit.Test;

/**
 * @author Chris Cranford
 */
public class JoinTableWithDefaultSchemaTest extends BaseUnitTestCase {

	@Test
	@TestForIssue(jiraKey = "HHH-10978")
	@RequiresDialect(SQLServerDialect.class)
	public void testGetTableDataForJoinTableWithDefaultSchema() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DEFAULT_CATALOG, "hibernate_orm_test" )
				.applySetting( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "false" )
				.build();
		try {
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( Task.class )
					.addAnnotatedClass( Project.class )
					.buildMetadata();
			metadata.validate();

			// first create the schema...
			new SchemaExport().create( EnumSet.of( TargetType.DATABASE ), metadata );
			try {
				// validate the schema
				new SchemaValidator().validate( metadata );
			}
			finally {
				// cleanup
				new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "Task")
	@Table(name = "tasks", schema = "dbo")
	public static class Task {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

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

		@Override
		public int hashCode() {
			int result = ( id != null ? id.hashCode() : 0 );
			result += 31 * result + ( name != null ? name.hashCode() : 0 );
			return result;
		}

		@Override
		public boolean equals(Object object) {
			if ( object == this ) {
				return true;
			}
			if ( object == null || !( object instanceof Task ) ) {
				return false;
			}
			Task task = (Task) object;
			if ( id != null ? !id.equals( task.id ) : task.id != null ) {
				return false;
			}
			return !( name != null ? !name.equals( task.name ) : task.name != null );
		}
	}

	@Entity(name = "Project")
	@Table(name = "projects", schema = "dbo")
	public static class Project {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
		@OneToMany
		@JoinTable(name = "project_tasks", schema="dbo")
		private List<Task> tasks = new ArrayList<Task>();

		@Override
		public int hashCode() {
			int result = ( id != null ? id.hashCode() : 0 );
			result += 31 * result + ( name != null ? name.hashCode() : 0 );
			return result;
		}

		@Override
		public boolean equals(Object object) {
			if ( object == this ) {
				return true;
			}
			if ( object == null || !( object instanceof Task ) ) {
				return false;
			}
			Project project = (Project) object;
			if ( id != null ? !id.equals( project.id ) : project.id != null ) {
				return false;
			}
			return !( name != null ? !name.equals( project.name ) : project.name != null );
		}
	}
}
