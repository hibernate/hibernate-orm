/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql;

import java.sql.Types;
import java.util.Map;

import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;

import org.jboss.logging.Logger;

/**
 * Manages a mapping between nationalized and non-nationalized variants of JDBC types.
 *
 * At the moment we only care about being able to map non-nationalized codes to the
 * corresponding nationalized equivalent, so that's all we implement for now
 *
 * @author Steve Ebersole
 */
public class NationalizedTypeMappings {
	private static final Logger log = Logger.getLogger( NationalizedTypeMappings.class );

	/**
	 * Singleton access
	 */
	public static final NationalizedTypeMappings INSTANCE = new NationalizedTypeMappings();

	private final Map<Integer,Integer> nationalizedCodeByNonNationalized;

	public NationalizedTypeMappings() {
		this.nationalizedCodeByNonNationalized =  new BoundedConcurrentHashMap<Integer, Integer>();
		map( Types.CHAR, Types.NCHAR );
		map( Types.CLOB, Types.NCLOB );
		map( Types.LONGVARCHAR, Types.LONGNVARCHAR );
		map( Types.VARCHAR, Types.NVARCHAR );
	}

	private void map(int nonNationalizedCode, int nationalizedCode) {
		nationalizedCodeByNonNationalized.put( nonNationalizedCode, nationalizedCode );
	}

	public int getCorrespondingNationalizedCode(int jdbcCode) {
		Integer nationalizedCode = nationalizedCodeByNonNationalized.get( jdbcCode );
		if ( nationalizedCode == null ) {
			log.debug( "Unable to locate nationalized jdbc-code equivalent for given jdbc code : " + jdbcCode );
			return jdbcCode;
		}
		return nationalizedCode;
	}
}
