/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.process.spi;

import java.sql.ResultSet;

import org.hibernate.loader.plan.exec.process.internal.ResultSetProcessingContextImpl;
import org.hibernate.loader.plan.spi.CollectionReference;

/**
 * @author Steve Ebersole
 */
public interface CollectionReferenceInitializer {
	// again, not sure.  ResultSetProcessingContextImpl.initializeEntitiesAndCollections() stuff?
	void finishUpRow(ResultSet resultSet, ResultSetProcessingContextImpl context);

	CollectionReference getCollectionReference();

	void endLoading(ResultSetProcessingContextImpl context);
}
