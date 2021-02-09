package org.hibernate.test.bulkid;

import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(DialectChecks.SupportsGlobalTemporaryTables.class)
public class GlobalTemporaryTableBulkCompositeIdTest extends AbstractBulkCompositeIdTest {

	@Override
	protected Class<? extends MultiTableBulkIdStrategy> getMultiTableBulkIdStrategyClass() {
		// Since we only allow dialects that support global temporary tables, we avoid overriding the strategy
		// This is important because otherwise we would loose id table configurations that are made in the dialects
		return null;
	}
}