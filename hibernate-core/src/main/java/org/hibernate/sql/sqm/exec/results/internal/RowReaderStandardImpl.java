/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.results.internal;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.sql.sqm.exec.results.spi.ResultSetProcessingOptions;
import org.hibernate.sql.sqm.exec.results.spi.ResultSetProcessingState;
import org.hibernate.sql.sqm.exec.results.spi.ReturnReader;
import org.hibernate.sql.sqm.exec.results.spi.RowProcessingState;
import org.hibernate.sql.sqm.exec.results.spi.RowReader;
import org.hibernate.sql.sqm.exec.spi.RowTransformer;
import org.hibernate.sql.sqm.convert.spi.Return;

/**
 * @author Steve Ebersole
 */
public class RowReaderStandardImpl<T> implements RowReader<T> {
	private final ReturnReader[] returnReaders;
	private final RowTransformer<T> rowTransformer;

	public RowReaderStandardImpl(List<Return> returns, RowTransformer<T> rowTransformer) {
		this.returnReaders = extractReturnReaders( returns );
		this.rowTransformer = rowTransformer;
	}

	private static ReturnReader[] extractReturnReaders(List<Return> returns) {
		final int count = returns.size();

		final ReturnReader[] returnReaders = new ReturnReader[ count ];
		for ( int i = 0; i < count; i++ ) {
			returnReaders[i] = returns.get( i ).getReturnReader();
		}

		return returnReaders;
	}

	@Override
	public T readRow(RowProcessingState processingState, ResultSetProcessingOptions options) throws SQLException {
		// NOTE : for now we assume very simple reads (basic values)

		final int returnCount = returnReaders.length;
		final Object[] row = new Object[returnCount];

		int position = 1;
		// first phase of reading
		for ( ReturnReader returnReader : returnReaders ) {
			returnReader.readBasicValues( processingState, options );
		}
		for ( ReturnReader returnReader : returnReaders ) {
			returnReader.resolveBasicValues( processingState, options );
		}
		for ( int i = 0; i < returnCount; i++ ) {
			row[i] = returnReaders[i].assemble( processingState, options );
		}

		return rowTransformer.transformRow( row );
	}

	@Override
	public void finishUp(ResultSetProcessingState context, List<AfterLoadAction> afterLoadActionList) {

	}
}
