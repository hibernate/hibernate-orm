/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.internal.instantiation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.sql.exec.results.process.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.exec.results.process.spi.RowProcessingState;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class ReturnAssemblerListImpl implements ReturnAssembler {
	private static final Logger log = Logger.getLogger( ReturnAssemblerListImpl.class );

	private final List<ArgumentReader> argumentReaders;

	public ReturnAssemblerListImpl(List<ArgumentReader> argumentReaders) {
		this.argumentReaders = argumentReaders;
	}

	@Override
	public Class getReturnedJavaType() {
		return List.class;
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) throws SQLException {
		final ArrayList<Object> result = new ArrayList<>();
		for ( ArgumentReader argumentReader : argumentReaders ) {
			result.add( argumentReader.assemble( rowProcessingState, options ) );
		}
		return result;
	}
}
