/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.query.spi.Limit;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FirebirdDialectTest {

	@ParameterizedTest
	@CsvSource(useHeadersInDisplayName = true, value = {
			"major, minor, offset, limit, expectedSQL",
			"2,     5,     ,       10,    select first ? * from tablename t where t.cat = 5",
			"2,     5,     10,     0,     select skip ? * from tablename t where t.cat = 5",
			"2,     5,     5,      10,    select first ? skip ? * from tablename t where t.cat = 5",
			"3,     0,     ,       10,    select * from tablename t where t.cat = 5 fetch first ? rows only",
			"3,     0,     10,     0,     select * from tablename t where t.cat = 5 offset ? rows",
			"3,     0,     5,      10,    select * from tablename t where t.cat = 5 offset ? rows fetch next ? rows only"
	})
	@JiraKey( "HHH-18213" )
	void insertOffsetLimitClause(int major, int minor, Integer offset, Integer limit, String expectedSql) {
		String input = "select * from tablename t where t.cat = 5";
		FirebirdDialect dialect = new FirebirdDialect( DatabaseVersion.make( major, minor ) );
		String actual = dialect.getLimitHandler().processSql( input, new Limit( offset, limit ) );
		assertEquals( expectedSql, actual );
	}
}
