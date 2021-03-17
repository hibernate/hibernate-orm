/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql;

import java.sql.Types;

import org.jboss.logging.Logger;

/**
 * Manages a mapping between nationalized and non-nationalized variants of JDBC types.
 *
 * At the moment we only care about being able to map non-nationalized codes to the
 * corresponding nationalized equivalent, so that's all we implement for now
 *
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
public final class NationalizedTypeMappings {

	private static final Logger log = Logger.getLogger( NationalizedTypeMappings.class );

	/**
	 * Singleton access
	 * @deprecated use the static methods instead
	 */
	@Deprecated
	public static final NationalizedTypeMappings INSTANCE = new NationalizedTypeMappings();

	private NationalizedTypeMappings() {
	}

	public static int toNationalizedTypeCode(final int jdbcCode) {
		switch ( jdbcCode ) {
			case Types.CHAR: return Types.NCHAR;
			case Types.CLOB: return Types.NCLOB;
			case Types.LONGVARCHAR: return Types.LONGNVARCHAR;
			case Types.VARCHAR: return Types.NVARCHAR;
			default:
				if ( log.isDebugEnabled() ) {
					log.debug( "Unable to locate nationalized jdbc-code equivalent for given jdbc code : " + jdbcCode );
				}
				return jdbcCode;
		}
	}

	/**
	 * @deprecated use {@link #toNationalizedTypeCode(int)}
	 * @param jdbcCode
	 * @return corresponding nationalized code
	 */
	@Deprecated
	public int getCorrespondingNationalizedCode(int jdbcCode) {
		return toNationalizedTypeCode( jdbcCode );
	}

}
