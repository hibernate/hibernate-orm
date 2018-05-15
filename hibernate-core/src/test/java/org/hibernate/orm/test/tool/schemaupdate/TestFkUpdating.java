/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;


/**
 * @author Steve Ebersole
 */
public class TestFkUpdating extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class, Role.class };
	}

	@SchemaTest
	public void testUpdate(SchemaScope schemaScope) {
		//First create the schema
		schemaScope.withSchemaExport( schemaExport ->
								  schemaExport.create( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ) ) );

		//Then try to update the scgema
		schemaScope.withSchemaUpdate( schemaUpdate ->
								  schemaUpdate.setHaltOnError( true )
										  .execute( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ) ) );
	}

	@Entity(name = "User")
	@Table(name = "my_user")
	public static class User {
		private Integer id;
		private Set<Role> roles;

		@Id
		@GeneratedValue(generator = "increment")
		@GenericGenerator(name = "increment", strategy = "increment")
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@ManyToMany
		public Set<Role> getRoles() {
			return roles;
		}

		public void setRoles(Set<Role> roles) {
			this.roles = roles;
		}
	}

	@Entity(name = "Role")
	@Table(name = "`Role`")
	public class Role {
		private Integer id;

		@Id
		@GeneratedValue(generator = "increment")
		@GenericGenerator(name = "increment", strategy = "increment")
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
}
