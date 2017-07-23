/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.internal.instantiation;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.sql.exec.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.sql.exec.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class QueryResultAssemblerListImpl implements QueryResultAssembler {
	private static final Logger log = Logger.getLogger( QueryResultAssemblerListImpl.class );

	private final BasicJavaDescriptor<List> listJavaDescriptor;
	private final List<ArgumentReader> argumentReaders;

	public QueryResultAssemblerListImpl(
			BasicJavaDescriptor<List> listJavaDescriptor,
			List<ArgumentReader> argumentReaders) {
		this.listJavaDescriptor = listJavaDescriptor;
		this.argumentReaders = argumentReaders;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return listJavaDescriptor;
	}

	@Override
	public Object assemble(
			RowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingOptions options) {
		final ArrayList<Object> result = new ArrayList<>();
		for ( ArgumentReader argumentReader : argumentReaders ) {
			result.add( argumentReader.assemble( rowProcessingState, options ) );
		}
		return result;
	}
}
