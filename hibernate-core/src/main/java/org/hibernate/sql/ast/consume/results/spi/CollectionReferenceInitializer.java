/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.results.spi;

import java.sql.ResultSet;

import org.hibernate.sql.ast.produce.result.spi.CollectionReference;

/**
 * @author Steve Ebersole
 */
public interface CollectionReferenceInitializer extends Initializer {
	// again, not sure.  ResultSetProcessingContextImpl.initializeEntitiesAndCollections() stuff?
	void finishUpRow(ResultSet resultSet, JdbcValuesSourceProcessingState processingState);

	CollectionReference getCollectionReference();

	void endLoading(JdbcValuesSourceProcessingState processingState);
}
