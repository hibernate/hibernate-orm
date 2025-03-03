/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

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
}
