/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.process.internal;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.loader.plan.exec.process.spi.ResultSetProcessingContext;
import org.hibernate.loader.plan.exec.process.spi.ReturnReader;
import org.hibernate.loader.plan.spi.CollectionReturn;

/**
 * @author Steve Ebersole
 */
public class CollectionReturnReader implements ReturnReader {
	private final CollectionReturn collectionReturn;

	public CollectionReturnReader(CollectionReturn collectionReturn) {
		this.collectionReturn = collectionReturn;
	}

	@Override
	public Object read(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
