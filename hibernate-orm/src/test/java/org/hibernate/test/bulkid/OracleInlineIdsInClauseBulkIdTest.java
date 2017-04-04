package org.hibernate.test.bulkid;

import org.hibernate.dialect.Oracle8iDialect;

import org.hibernate.testing.RequiresDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(Oracle8iDialect.class)
public class OracleInlineIdsInClauseBulkIdTest extends InlineIdsInClauseBulkIdTest {

	@Override
	protected int entityCount() {
		return 1100;
	}
}