package org.hibernate.spatial.testing.dialects.mysql;

import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.DataSourceUtils;

/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: 10/9/13
 */
public class MySQL56TestSupport extends MySQLTestSupport {

	@Override
	public AbstractExpectationsFactory createExpectationsFactory(DataSourceUtils dataSourceUtils) {
		return new MySQL56ExpectationsFactory( dataSourceUtils );
	}
}
