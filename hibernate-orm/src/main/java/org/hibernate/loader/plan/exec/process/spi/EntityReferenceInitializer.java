/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.process.spi;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.loader.plan.exec.process.internal.ResultSetProcessingContextImpl;
import org.hibernate.loader.plan.spi.EntityReference;

/**
 * @author Steve Ebersole
 */
public interface EntityReferenceInitializer {
	EntityReference getEntityReference();

	void hydrateIdentifier(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException;

	void resolveEntityKey(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException;

	void hydrateEntityState(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException;

	void finishUpRow(ResultSet resultSet, ResultSetProcessingContextImpl context) throws SQLException;
}
