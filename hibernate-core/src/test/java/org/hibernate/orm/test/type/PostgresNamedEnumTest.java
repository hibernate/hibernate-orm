/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import java.util.Locale;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

@RequiresDialect(PostgreSQLDialect.class)
public class PostgresNamedEnumTest extends AbstractNamedEnumTest {

	@Override
	protected String normalizeNameForQueryingMetadata(String name) {
		return name.toLowerCase(Locale.ROOT);
	}

	@Override
	protected String getDataTypeForNamedEnum(String namedEnum) {
		return namedEnum.toLowerCase(Locale.ROOT);
	}

	@Override
	protected String getDataTypeForNamedOrdinalEnum(String namedEnum) {
		return namedEnum.toLowerCase(Locale.ROOT);
	}

}
