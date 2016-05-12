/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemavalidation.matchingtablenames;

import static org.junit.Assert.fail;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.extract.spi.SchemaExtractionException;
import org.junit.Test;

/**
 * Test case for HHH-10718. It is possible to define table names such that a JDBC
 * database metadata lookup on one table name will return multiple rows. The example
 * here has two entity classes with corresponding tables AN_ENTITY and ANOENTITY. 
 * Since the underscore character is a single-character wildcard, looking up the 
 * table metadata for the first of these will also return a row for the latter,
 * as the java.sql.DatabaseMetaData.getTables treats the specified table name as 
 * a pattern and hence finds tables whose name is "LIKE" that given. 
 * 
 * This would result in an exception being thrown ... 
 * 
 * org.hibernate.tool.schema.extract.spi.SchemaExtractionException: More than one table found in namespace 
 * 
 * ... from the method InformationExtractorJdbcDatabaseMetaDataImpl.processGetTableResults 
 * which was written to expect only a single row of metadata when looking up a table, 
 * and to throw an exception in the case of multiple rows within the result set. 
 * 
 * @author Richard Barnes 3 May 2016
 */
public class MatchingTablenamesTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				AnEntity.class,
				AnotherEntity.class
		};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10718")
	public void testForApparentDuplicateTables() {
		try {
			new SchemaValidator().validate( metadata() );
		}
		catch (SchemaExtractionException e) {
			fail( e.getMessage() );
		}
	}
}
