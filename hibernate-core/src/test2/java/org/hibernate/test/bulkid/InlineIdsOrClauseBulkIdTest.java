package org.hibernate.test.bulkid;

import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.inline.InlineIdsOrClauseBulkIdStrategy;

/**
 * @author Vlad Mihalcea
 */
public class InlineIdsOrClauseBulkIdTest extends AbstractBulkIdTest {

	@Override
	protected Class<? extends MultiTableBulkIdStrategy> getMultiTableBulkIdStrategyClass() {
		return InlineIdsOrClauseBulkIdStrategy.class;
	}
}