/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.foreignkeys.definition;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(value = H2Dialect.class)
public class ForeignKeyDefinitionSecondaryTableTest
		extends AbstractForeignKeyDefinitionTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				User.class,
		};
	}

	@Entity(name = "User")
	@Table(name = "USERS")
	@SecondaryTable(name = "User_details", foreignKey = @ForeignKey(name = "secondary", foreignKeyDefinition = "foreign key /* FK */ (id) references Users"))
	public class User {

		@Id
		@GeneratedValue
		private int id;

		private String emailAddress;

		@Column(name = "SECURITY_USERNAME", table = "User_details")
		private String username;

		@Column(name = "SECURITY_PASSWORD", table = "User_details")
		private String password;
	}


	@Override
	protected boolean validate(String fileContent) {
		return fileContent.contains( "/* FK */" );
	}
}
