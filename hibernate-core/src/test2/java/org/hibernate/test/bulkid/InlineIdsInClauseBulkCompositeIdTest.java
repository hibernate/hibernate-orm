package org.hibernate.test.bulkid;

import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.inline.InlineIdsInClauseBulkIdStrategy;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(DialectChecks.SupportRowValueConstructorSyntaxInInList.class)
public class InlineIdsInClauseBulkCompositeIdTest extends
		AbstractBulkCompositeIdTest {

	@Override
	protected Class<? extends MultiTableBulkIdStrategy> getMultiTableBulkIdStrategyClass() {
		return InlineIdsInClauseBulkIdStrategy.class;
	}
}