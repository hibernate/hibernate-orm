/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal;

import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.sql.exec.results.process.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.exec.results.process.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.exec.results.process.spi.RowProcessingState;
import org.hibernate.sql.exec.results.process.spi.RowReader;

/**
 * @author Steve Ebersole
 */
public class RowReaderNoResultsExpectedImpl implements RowReader {
	/**
	 * Singleton access
	 */
	public static final RowReaderNoResultsExpectedImpl INSTANCE = new RowReaderNoResultsExpectedImpl();

	public static <R> RowReader<R> instance() {
		return INSTANCE;
	}

	private RowReaderNoResultsExpectedImpl() {
	}

	@Override
	public Object readRow(RowProcessingState processingState, JdbcValuesSourceProcessingOptions options) throws SQLException {
		throw new HibernateException( "Not expecting results" );
	}

	@Override
	public void finishUp(JdbcValuesSourceProcessingState context) {
		// nothing to do
	}
}
