/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate;

/**
 * Defines the representation modes available for entities.
 *
 * @author Steve Ebersole
 */
public enum EntityMode {
	POJO( "pojo" ),
	MAP( "dynamic-map" );

	private final String name;

	EntityMode(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	private static final String DYNAMIC_MAP_NAME = MAP.name.toUpperCase();

	/**
	 * Legacy-style entity-mode name parsing.  <b>Case insensitive</b>
	 *
	 * @param entityMode The entity mode name to evaluate
	 *
	 * @return The appropriate entity mode; {@code null} for incoming {@code entityMode} param is treated by returning
	 * {@link #POJO}.
	 */
	public static EntityMode parse(String entityMode) {
		if ( entityMode == null ) {
			return POJO;
		}
		entityMode = entityMode.toUpperCase();
		if ( DYNAMIC_MAP_NAME.equals( entityMode ) ) {
			return MAP;
		}
		return valueOf( entityMode );
	}

}
