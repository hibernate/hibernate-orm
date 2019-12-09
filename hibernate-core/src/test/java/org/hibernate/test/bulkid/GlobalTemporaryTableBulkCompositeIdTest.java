package org.hibernate.test.bulkid;

import org.hibernate.dialect.CockroachDB1920Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.inline.InlineIdsInClauseBulkIdStrategy;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQL82Dialect.class)
@SkipForDialect(value = CockroachDB1920Dialect.class, comment = "https://github.com/cockroachdb/cockroach/issues/5807")
public class GlobalTemporaryTableBulkCompositeIdTest extends AbstractBulkCompositeIdTest {

	@Override
	protected Class<? extends MultiTableBulkIdStrategy> getMultiTableBulkIdStrategyClass() {
		return GlobalTemporaryTableBulkIdStrategy.class;
	}
}