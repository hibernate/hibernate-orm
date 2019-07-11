package org.hibernate.test.bulkid;

/**
 * @author Vlad Mihalcea
 */
public class InlineIdsOrClauseBulkCompositeIdTest extends
		AbstractBulkCompositeIdTest {

	@Override
	protected Class<? extends MultiTableBulkIdStrategy> getMultiTableBulkIdStrategyClass() {
		return InlineIdsOrClauseBulkIdStrategy.class;
	}
}