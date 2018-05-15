package org.hibernate.test.bulkid;

import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.inline.InlineIdsInClauseBulkIdStrategy;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.RequiresDialectFeature;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQL82Dialect.class)
public class GlobalTemporaryTableBulkCompositeIdTest extends AbstractBulkCompositeIdTest {

	@Override
	protected Class<? extends MultiTableBulkIdStrategy> getMultiTableBulkIdStrategyClass() {
		return GlobalTemporaryTableBulkIdStrategy.class;
	}
}