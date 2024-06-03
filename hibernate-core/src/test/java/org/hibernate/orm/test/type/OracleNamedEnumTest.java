/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.fail;

@RequiresDialect(value = OracleDialect.class, majorVersion = 23)
public class OracleNamedEnumTest extends AbstractNamedEnumTest {

	@Override
	protected String normalizeNameForQueryingMetadata(String name) {
		return name.toUpperCase(Locale.ROOT);
	}

	@Override
	protected String getDataTypeForNamedEnum(String namedEnum) {
		return "VARCHAR2";
	}

	@Override
	protected String getDataTypeForNamedOrdinalEnum(String namedEnum) {
		return "NUMBER";
	}

	@Test public void testUserDomains(SessionFactoryScope scope) {
		scope.inSession( s -> {
			s.doWork(
					c -> {
						boolean namedEnumFound = false;
						boolean namedOrdinalEnumFound = false;

						try(Statement stmt = c.createStatement()) {
							try(ResultSet typeInfo = stmt.executeQuery("select name, decode(instr(data_display,'WHEN '''),0,'NUMBER','VARCHAR2') from user_domains where type='ENUMERATED'")) {
								while (typeInfo.next()) {
									String name = typeInfo.getString(1);
									String baseType = typeInfo.getString(2);
									if (name.equalsIgnoreCase("ActivityType") && baseType.equals(
											getDataTypeForNamedEnum("ActivityType"))) {
										namedEnumFound = true;
										continue;
									}
									if (name.equalsIgnoreCase("SkyType") && baseType.equals(
											getDataTypeForNamedOrdinalEnum("SkyType"))) {
										namedOrdinalEnumFound = true;
									}
								}
							}
						}

						if (!namedEnumFound) {
							fail("named enum type not exported");
						}
						if (!namedOrdinalEnumFound) {
							fail("named ordinal enum type not exported");
						}
					}
			);
		});
	}
}
