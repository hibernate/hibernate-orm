package org.hibernate.hql.ast;

import org.hibernate.hql.ParameterTranslations;
import org.hibernate.type.Type;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.param.PositionalParameterSpecification;
import org.hibernate.param.NamedParameterSpecification;
import org.hibernate.util.ArrayHelper;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.io.Serializable;

/**
 * Defines the information available for parameters encountered during
 * query translation through the antlr-based parser.
 *
 * @author Steve Ebersole
 */
public class ParameterTranslationsImpl implements ParameterTranslations {

	private final Map namedParameters;
	private final ParameterInfo[] ordinalParameters;

	public boolean supportsOrdinalParameterMetadata() {
		return true;
	}

	public int getOrdinalParameterCount() {
		return ordinalParameters.length;
	}

	public ParameterInfo getOrdinalParameterInfo(int ordinalPosition) {
		// remember that ordinal parameters numbers are 1-based!!!
		return ordinalParameters[ordinalPosition - 1];
	}

	public int getOrdinalParameterSqlLocation(int ordinalPosition) {
		return getOrdinalParameterInfo( ordinalPosition ).getSqlLocations()[0];
	}

	public Type getOrdinalParameterExpectedType(int ordinalPosition) {
		return getOrdinalParameterInfo( ordinalPosition ).getExpectedType();
	}

	public Set getNamedParameterNames() {
		return namedParameters.keySet();
	}

	public ParameterInfo getNamedParameterInfo(String name) {
		return ( ParameterInfo ) namedParameters.get( name );
	}

	public int[] getNamedParameterSqlLocations(String name) {
		return getNamedParameterInfo( name ).getSqlLocations();
	}

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
	 * @param parameterSpecifications
	 */
	public ParameterTranslationsImpl(List parameterSpecifications) {

		class NamedParamTempHolder {
			String name;
			Type type;
			List positions = new ArrayList();
		}

		int size = parameterSpecifications.size();
		List ordinalParameterList = new ArrayList();
		Map namedParameterMap = new HashMap();
		for ( int i = 0; i < size; i++ ) {
			final ParameterSpecification spec = ( ParameterSpecification ) parameterSpecifications.get( i );
			if ( PositionalParameterSpecification.class.isAssignableFrom( spec.getClass() ) ) {
				PositionalParameterSpecification ordinalSpec = ( PositionalParameterSpecification ) spec;
				ordinalParameterList.add( new ParameterInfo( i, ordinalSpec.getExpectedType() ) );
			}
			else if ( NamedParameterSpecification.class.isAssignableFrom( spec.getClass() ) ) {
				NamedParameterSpecification namedSpec = ( NamedParameterSpecification ) spec;
				NamedParamTempHolder paramHolder = ( NamedParamTempHolder ) namedParameterMap.get( namedSpec.getName() );
				if ( paramHolder == null ) {
					paramHolder = new NamedParamTempHolder();
					paramHolder.name = namedSpec.getName();
					paramHolder.type = namedSpec.getExpectedType();
					namedParameterMap.put( namedSpec.getName(), paramHolder );
				}
				paramHolder.positions.add( new Integer( i ) );
			}
			else {
				// don't care about other param types here, just those explicitly user-defined...
			}
		}

		ordinalParameters = ( ParameterInfo[] ) ordinalParameterList.toArray( new ParameterInfo[ordinalParameterList.size()] );

		if ( namedParameterMap.isEmpty() ) {
			namedParameters = java.util.Collections.EMPTY_MAP;
		}
		else {
			Map namedParametersBacking = new HashMap( namedParameterMap.size() );
			Iterator itr = namedParameterMap.values().iterator();
			while( itr.hasNext() ) {
				final NamedParamTempHolder holder = ( NamedParamTempHolder ) itr.next();
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
