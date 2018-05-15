/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
public class QuotedTableNameWithForeignKeysSchemaUpdateAnnottationTest extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Group.class, User.class };
	}

	@Override
	protected void beforeEach(SchemaScope scope) {
		scope.withSchemaUpdate( schemaUpdate ->
								{
									schemaUpdate.execute( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ) );
									assertThat(
											"An unexpected Exception occurred during the database schema update",
											schemaUpdate.getExceptions().size(),
											is( 0 )
									);
								} );
	}

	@SchemaTest
	public void testUpdateExistingSchema(SchemaScope scope) {
		scope.withSchemaUpdate( schemaUpdate -> {
			schemaUpdate.execute( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ) );
			assertThat(
					"An unexpected Exception occurred during the database schema update",
					schemaUpdate.getExceptions().size(),
					is( 0 )
			);
		} );
	}

	@Entity(name = "User")
	@Table(name = "`User`")
	@IdClass(CompositeId.class)
	public static class User implements Serializable {

		@Id
		private String org;
		@Id
		private String name;

		@ManyToMany(mappedBy = "users")
		private Set<Group> groups = new HashSet();
	}

	@Entity(name = "Group")
	@Table(name = "`Group`")
	@IdClass(CompositeId.class)
	public static class Group implements Serializable {

		@Id
		private String org;
		@Id
		private String name;

		private String description;

		@ManyToMany
		@JoinTable(name = "`UserGroup`",
				joinColumns = {
						@JoinColumn(name = "groupName"),
						@JoinColumn(name = "groupOrg")
				},
				inverseJoinColumns = {
						@JoinColumn(name = "userName"),
						@JoinColumn(name = "userOrg")
				})
		private Set<User> users = new HashSet();
	}

	public static class CompositeId {
		private String org;
		private String name;

		public String getOrg() {
			return org;
		}

		public void setOrg(String org) {
			this.org = org;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
