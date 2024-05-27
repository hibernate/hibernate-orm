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
