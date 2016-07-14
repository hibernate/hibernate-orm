/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.results.spi;

import java.sql.SQLException;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Contract for reading an individual return (selection) from the underlying ResultSet
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ReturnReader<T> {
	// proposal for the contract for reading back values
	//		relies on Return/ReturnReader understanding the overall positions of its values
	// 		in the ResultSet which may or may not be feasible.

	void readBasicValues(RowProcessingState processingState, ResultSetProcessingOptions options) throws SQLException;
	void resolveBasicValues(RowProcessingState processingState, ResultSetProcessingOptions options) throws SQLException;
	T assemble(RowProcessingState processingState, ResultSetProcessingOptions options) throws SQLException;

	Class<T> getReturnedJavaType();

	int getNumberOfColumnsRead(SessionFactoryImplementor sessionFactory);
}
