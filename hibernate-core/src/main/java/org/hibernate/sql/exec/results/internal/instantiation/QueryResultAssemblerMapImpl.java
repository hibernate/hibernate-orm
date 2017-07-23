/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.internal.instantiation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.sql.exec.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.sql.exec.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * A ReturnAssembler implementation for handling dynamic-instantiations targeting a Map
 *
 * @author Steve Ebersole
 */
public class QueryResultAssemblerMapImpl implements QueryResultAssembler {
	private final BasicJavaDescriptor<Map> mapJavaDescriptor;
	private final List<ArgumentReader> argumentReaders;

	public QueryResultAssemblerMapImpl(
			BasicJavaDescriptor<Map> mapJavaDescriptor,
			List<ArgumentReader> argumentReaders) {
		this.mapJavaDescriptor = mapJavaDescriptor;
		this.argumentReaders = argumentReaders;

		final Set<String> aliases = new HashSet<>();
		for ( ArgumentReader argumentReader : argumentReaders ) {
			if ( argumentReader.getAlias() == null ) {
				throw new IllegalStateException( "alias for Map dynamic instantiation argument cannot be null" );
			}

			if ( ! aliases.add( argumentReader.getAlias() ) ) {
				throw new IllegalStateException( "Encountered duplicate alias for Map dynamic instantiation argument [" + argumentReader.getAlias() + "]" );
			}
		}

	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return mapJavaDescriptor;
	}

	@Override
	public Object assemble(
			RowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingOptions options) {
		final HashMap<String,Object> result = new HashMap<>();

		for ( ArgumentReader argumentReader : argumentReaders ) {
			result.put(
					argumentReader.getAlias(),
					argumentReader.assemble( rowProcessingState, options )
			);
		}

		return result;
	}
}
