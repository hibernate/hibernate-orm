/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys.definition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
public class ForeignKeyDefinitionSecondaryTableTest extends AbstractForeignKeyDefinitionTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { User.class };
	}

	@Override
	protected boolean validate(String fileContent) {
		return fileContent.contains( "/* FK */" );
	}

	@Entity(name = "User")
	@Table(name = "USERS")
	@SecondaryTable(name = "User_details", foreignKey = @ForeignKey(name = "secondary", foreignKeyDefinition = "foreign key /* FK */ (id) references Users"))
	public static class User {

		@Id
		@GeneratedValue
		private int id;

		private String emailAddress;

		@Column(name = "SECURITY_USERNAME", table = "User_details")
		private String username;

		@Column(name = "SECURITY_PASSWORD", table = "User_details")
		private String password;
	}
}
