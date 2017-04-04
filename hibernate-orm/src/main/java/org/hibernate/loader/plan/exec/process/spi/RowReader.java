/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.process.spi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.loader.plan.exec.process.internal.ResultSetProcessingContextImpl;
import org.hibernate.loader.spi.AfterLoadAction;

/**
 * @author Steve Ebersole
 */
public interface RowReader {
	// why the context *impl*?
	Object readRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException;

	void finishUp(ResultSetProcessingContextImpl context, List<AfterLoadAction> afterLoadActionList);
}
