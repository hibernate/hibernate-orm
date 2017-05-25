/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.datetime;

import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Philippe Marschall
 */
@RequiresDialect(Oracle9iDialect.class)
public class OracleTimestampWithTimeZoneSchemaGenerationTest extends AbstractTimestampWithTimeZoneSchemaGenerationTest {

	@Override
	protected String expectedTableDefinition() {
		return "create table log_entry (id number(10,0) not null, created_date timestamp with time zone, start_shift time, primary key (id))";
	}
}
