/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.datetime;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Philippe Marschall
 */
@RequiresDialect(H2Dialect.class)
public class H2TimestampWithTimeZoneSchemaGenerationTest extends AbstractTimestampWithTimeZoneSchemaGenerationTest {

	@Override
	protected String expectedTableDefinition() {
		return "create table log_entry (id integer not null, created_date timestamp with time zone, start_shift time, primary key (id))";
	}
}
