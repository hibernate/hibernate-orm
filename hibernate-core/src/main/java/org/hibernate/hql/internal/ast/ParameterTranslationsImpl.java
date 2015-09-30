/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.hql.spi.ParameterTranslations;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.param.NamedParameterSpecification;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.param.PositionalParameterSpecification;
import org.hibernate.type.Type;

/**
 * Defines the information available for parameters encountered during
 * query translation through the antlr-based parser.
 *
 * @author Steve Ebersole
 */
public class ParameterTranslationsImpl implements ParameterTranslations {
	private final Map<String,ParameterInfo> namedParameters;
	private final ParameterInfo[] ordinalParameters;

	@Override
	public boolean supportsOrdinalParameterMetadata() {
		return true;
	}

	@Override
	public int getOrdinalParameterCount() {
		return ordinalParameters.length;
	}

	public ParameterInfo getOrdinalParameterInfo(int ordinalPosition) {
		// remember that ordinal parameters numbers are 1-based!!!
		return ordinalParameters[ordinalPosition - 1];
	}

	@Override
	public int getOrdinalParameterSqlLocation(int ordinalPosition) {
		return getOrdinalParameterInfo( ordinalPosition ).getSqlLocations()[0];
	}

	@Override
	public Type getOrdinalParameterExpectedType(int ordinalPosition) {
		return getOrdinalParameterInfo( ordinalPosition ).getExpectedType();
	}

	@Override
	public Set getNamedParameterNames() {
		return namedParameters.keySet();
	}

	public ParameterInfo getNamedParameterInfo(String name) {
		return namedParameters.get( name );
	}

	@Override
	public int[] getNamedParameterSqlLocations(String name) {
		return getNamedParameterInfo( name ).getSqlLocations();
	}

	@Override
	public Type getNamedParameterExpectedType(String name) {
		return getNamedParameterInfo( name ).getExpectedType();
	}

	/**
	 * Constructs a parameter metadata object given a list of parameter
	 * specifications.
	 * </p>
	 * Note: the order in the incoming list denotes the parameter's
	 * psudeo-position within the resulting sql statement.
	 *
	 * @param parameterSpecifications The parameter specifications
	 */
	public ParameterTranslationsImpl(List<ParameterSpecification> parameterSpecifications) {
		class NamedParamTempHolder {
			String name;
			Type type;
			List<Integer> positions = new ArrayList<Integer>();
		}

		final int size = parameterSpecifications.size();
		final List<ParameterInfo> ordinalParameterList = new ArrayList<ParameterInfo>();
		final Map<String,NamedParamTempHolder> namedParameterMap = new HashMap<String,NamedParamTempHolder>();
		for ( int i = 0; i < size; i++ ) {
			final ParameterSpecification spec = parameterSpecifications.get( i );
			if ( PositionalParameterSpecification.class.isInstance( spec ) ) {
				final PositionalParameterSpecification ordinalSpec = (PositionalParameterSpecification) spec;
				ordinalParameterList.add( new ParameterInfo( i, ordinalSpec.getExpectedType() ) );
			}
			else if ( NamedParameterSpecification.class.isInstance( spec ) ) {
				final NamedParameterSpecification namedSpec = (NamedParameterSpecification) spec;
				NamedParamTempHolder paramHolder = namedParameterMap.get( namedSpec.getName() );
				if ( paramHolder == null ) {
					paramHolder = new NamedParamTempHolder();
					paramHolder.name = namedSpec.getName();
					paramHolder.type = namedSpec.getExpectedType();
					namedParameterMap.put( namedSpec.getName(), paramHolder );
				}
				else if ( paramHolder.type == null && namedSpec.getExpectedType() != null ) {
					// previous reference to the named parameter did not have type determined;
					// this time, it can be determined by namedSpec.getExpectedType().
					paramHolder.type = namedSpec.getExpectedType();
				}
				paramHolder.positions.add( i );
			}
			// don't care about other param types here, just those explicitly user-defined...
		}

		ordinalParameters = ordinalParameterList.toArray( new ParameterInfo[ordinalParameterList.size()] );

		if ( namedParameterMap.isEmpty() ) {
			namedParameters = java.util.Collections.emptyMap();
		}
		else {
			final Map<String,ParameterInfo> namedParametersBacking = new HashMap<String,ParameterInfo>( namedParameterMap.size() );
			for ( NamedParamTempHolder holder : namedParameterMap.values() ) {
				namedParametersBacking.put(
						holder.name,
						new ParameterInfo( ArrayHelper.toIntArray( holder.positions ), holder.type )
				);
			}
			namedParameters = java.util.Collections.unmodifiableMap( namedParametersBacking );
		}
	}

	public static class ParameterInfo implements Serializable {
		private final int[] sqlLocations;
		private final Type expectedType;

		public ParameterInfo(int[] sqlPositions, Type expectedType) {
			this.sqlLocations = sqlPositions;
			this.expectedType = expectedType;
		}

		public ParameterInfo(int sqlPosition, Type expectedType) {
			this.sqlLocations = new int[] { sqlPosition };
			this.expectedType = expectedType;
		}

		public int[] getSqlLocations() {
			return sqlLocations;
		}

		public Type getExpectedType() {
			return expectedType;
		}
	}
}
