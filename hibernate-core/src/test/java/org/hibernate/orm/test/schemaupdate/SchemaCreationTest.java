/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate;

import java.util.EnumSet;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.tool.schema.TargetType;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class SchemaCreationTest extends BaseSchemaTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{Employee.class };
	}

	@Override
	protected boolean createTempOutputFile() {
		return true;
	}

	@Test
	public void testSchemaUpdateWithQuotedColumnNames() throws Exception {
		createSchemaUpdate().setHaltOnError( true ).execute( EnumSet.of( TargetType.SCRIPT ) );

		final String fileContent = getOutputFileContent();
		assertThat( "The update output file should be empty", fileContent, is( "" ) );
	}

	@Entity
	@Table(name = "Employee")
	public class Employee {
		@Id
		private long id;

		@Column(name = "`Age`")
		public String age;

		@Column(name = "Name")
		private String name;

		private String match;

		private String birthday;

		private String homeAddress;
	}
}
