/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
