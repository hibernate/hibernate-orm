/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.internal.instantiation;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.sql.exec.results.process.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.exec.results.process.spi.RowProcessingState;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;

/**
 * A ReturnAssembler implementation for handling dynamic-instantiations targeting a Map
 *
 * @author Steve Ebersole
 */
public class ReturnAssemblerMapImpl implements ReturnAssembler {
	private final List<ArgumentReader> argumentReaders;

	public ReturnAssemblerMapImpl(List<ArgumentReader> argumentReaders) {
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
	public Class getReturnedJavaType() {
		return Map.class;
	}

	@Override
	public Object assemble(
			RowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingOptions options) throws SQLException {
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
