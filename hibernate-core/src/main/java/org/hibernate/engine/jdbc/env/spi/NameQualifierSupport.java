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
package org.hibernate.engine.jdbc.env.spi;

/**
 * Enumerated values representing the level of support for catalog and schema.
 *
 * @author Steve Ebersole
 */
public enum NameQualifierSupport {
	/**
	 * Only catalog is supported
	 */
	CATALOG,
	/**
	 * Only schema is supported
	 */
	SCHEMA,
	/**
	 * Both catalog and schema are supported.
	 */
	BOTH,
	/**
	 * Neither catalog nor schema are supported.
	 */
	NONE;

	public boolean supportsCatalogs() {
		return this == CATALOG || this == BOTH;
	}

	public boolean supportsSchemas() {
		return this == SCHEMA || this == BOTH;
	}
}
