/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.binding;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;


/**
 * @author Hardy Ferentschik
 * @todo integrate this w/ org.hibernate.engine.spi.CascadeStyle
 */
public enum CascadeType {
	/**
	 * Cascades save, delete, update, evict, lock, replicate, merge, persist
	 */
	ALL,

	/**
	 * Cascades save, delete, update, evict, lock, replicate, merge, persist + delete orphans
	 */
	ALL_DELETE_ORPHAN,

	/**
	 * Cascades save and update
	 */
	UPDATE,

	/**
	 * Cascades persist
	 */
	PERSIST,

	/**
	 * Cascades merge
	 */
	MERGE,

	/**
	 * Cascades lock
	 */
	LOCK,

	/**
	 * Cascades refresh
	 */
	REFRESH,

	/**
	 * Cascades replicate
	 */
	REPLICATE,

	/**
	 * Cascades evict
	 */
	EVICT,

	/**
	 * Cascade delete
	 */
	DELETE,

	/**
	 * Cascade delete + delete orphans
	 */
	DELETE_ORPHAN,

	/**
	 * No cascading
	 */
	NONE;

	private static final Map<String, CascadeType> hbmOptionToCascadeType = new HashMap<String, CascadeType>();

	static {
		hbmOptionToCascadeType.put( "all", ALL );
		hbmOptionToCascadeType.put( "all-delete-orphan", ALL_DELETE_ORPHAN );
		hbmOptionToCascadeType.put( "save-update", UPDATE );
		hbmOptionToCascadeType.put( "persist", PERSIST );
		hbmOptionToCascadeType.put( "merge", MERGE );
		hbmOptionToCascadeType.put( "lock", LOCK );
		hbmOptionToCascadeType.put( "refresh", REFRESH );
		hbmOptionToCascadeType.put( "replicate", REPLICATE );
		hbmOptionToCascadeType.put( "evict", EVICT );
		hbmOptionToCascadeType.put( "delete", DELETE );
		hbmOptionToCascadeType.put( "remove", DELETE ); // adds remove as a sort-of alias for delete...
		hbmOptionToCascadeType.put( "delete-orphan", DELETE_ORPHAN );
		hbmOptionToCascadeType.put( "none", NONE );
	}

	private static final Map<javax.persistence.CascadeType, CascadeType> jpaCascadeTypeToHibernateCascadeType = new HashMap<javax.persistence.CascadeType, CascadeType>();

	static {
		jpaCascadeTypeToHibernateCascadeType.put( javax.persistence.CascadeType.ALL, ALL );
		jpaCascadeTypeToHibernateCascadeType.put( javax.persistence.CascadeType.PERSIST, PERSIST );
		jpaCascadeTypeToHibernateCascadeType.put( javax.persistence.CascadeType.MERGE, MERGE );
		jpaCascadeTypeToHibernateCascadeType.put( javax.persistence.CascadeType.REFRESH, REFRESH );
		jpaCascadeTypeToHibernateCascadeType.put( javax.persistence.CascadeType.DETACH, EVICT );
	}

	private static final Map<CascadeType, CascadeStyle> cascadeTypeToCascadeStyle = new HashMap<CascadeType, CascadeStyle>();
	static {
		cascadeTypeToCascadeStyle.put( ALL, CascadeStyles.ALL );
		cascadeTypeToCascadeStyle.put( ALL_DELETE_ORPHAN, CascadeStyles.ALL_DELETE_ORPHAN );
		cascadeTypeToCascadeStyle.put( UPDATE, CascadeStyles.UPDATE );
		cascadeTypeToCascadeStyle.put( PERSIST, CascadeStyles.PERSIST );
		cascadeTypeToCascadeStyle.put( MERGE, CascadeStyles.MERGE );
		cascadeTypeToCascadeStyle.put( LOCK, CascadeStyles.LOCK );
		cascadeTypeToCascadeStyle.put( REFRESH, CascadeStyles.REFRESH );
		cascadeTypeToCascadeStyle.put( REPLICATE, CascadeStyles.REPLICATE );
		cascadeTypeToCascadeStyle.put( EVICT, CascadeStyles.EVICT );
		cascadeTypeToCascadeStyle.put( DELETE, CascadeStyles.DELETE );
		cascadeTypeToCascadeStyle.put( DELETE_ORPHAN, CascadeStyles.DELETE_ORPHAN );
		cascadeTypeToCascadeStyle.put( NONE, CascadeStyles.NONE );
	}

	/**
	 * @param hbmOptionName the cascading option as specified in the hbm mapping file
	 *
	 * @return Returns the {@code CascadeType} for a given hbm cascading option
	 */
	public static CascadeType getCascadeType(String hbmOptionName) {
		return hbmOptionToCascadeType.get( hbmOptionName );
	}

	/**
	 * @param jpaCascade the jpa cascade type
	 *
	 * @return Returns the Hibernate {@code CascadeType} for a given jpa cascade type
	 */
	public static CascadeType getCascadeType(javax.persistence.CascadeType jpaCascade) {
		return jpaCascadeTypeToHibernateCascadeType.get( jpaCascade );
	}

	/**
	 * @return Returns the {@code CascadeStyle} that corresponds to this {@code CascadeType}
	 *
	 * @throws MappingException if there is not corresponding {@code CascadeStyle}
	 */
	public CascadeStyle toCascadeStyle() {
		CascadeStyle cascadeStyle = cascadeTypeToCascadeStyle.get( this );
		if ( cascadeStyle == null ) {
			throw new MappingException( "No CascadeStyle that corresponds with CascadeType=" + this.name() );
		}
		return cascadeStyle;
	}
}
