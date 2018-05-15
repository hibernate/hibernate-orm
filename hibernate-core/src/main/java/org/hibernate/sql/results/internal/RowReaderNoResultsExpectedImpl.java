/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.spi.RowReaderMemento;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.sql.results.spi.RowReader;

/**
 * @author Steve Ebersole
 */
public class RowReaderNoResultsExpectedImpl implements RowReader {
	/**
	 * Singleton access
	 */
	public static final RowReaderNoResultsExpectedImpl INSTANCE = new RowReaderNoResultsExpectedImpl();

	private static final RowReaderMemento MEMENTO = new RowReaderMemento() {
		@Override
		public Class[] getResultClasses() {
			return new Class[0];
		}

		@Override
		public String[] getResultMappingNames() {
			return new String[0];
		}
	};


	public static <R> RowReader<R> instance() {
		return INSTANCE;
	}

	private RowReaderNoResultsExpectedImpl() {
	}

	@Override
	public Class getResultJavaType() {
		return Void.TYPE;
	}

	@Override
	public List<Initializer> getInitializers() {
		return Collections.emptyList();
	}

	@Override
	public int getNumberOfResults() {
		return 0;
	}

	@Override
	public Object readRow(RowProcessingState processingState, JdbcValuesSourceProcessingOptions options) throws SQLException {
		throw new HibernateException( "Not expecting results" );
	}

	@Override
	public void finishUp(JdbcValuesSourceProcessingState context) {
		// nothing to do
	}

	@Override
	public RowReaderMemento toMemento(SessionFactoryImplementor factory) {
		return MEMENTO;
	}
}
