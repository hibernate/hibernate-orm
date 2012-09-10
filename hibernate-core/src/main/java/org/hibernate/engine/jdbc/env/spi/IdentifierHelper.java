/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import org.hibernate.metamodel.spi.relational.Identifier;

/**
 * Helper for handling {@link Identifier} instances.
 *
 * @author Steve Ebersole
 */
public interface IdentifierHelper {
	/**
	 * Generate an {@link Identifier} instance from its simple name
	 *
	 * @param text The text
	 *
	 * @return The identifier form of the name.
	 */
	public Identifier toIdentifier(String text);

	/**
	 * Generate an {@link Identifier} instance from its simple name and explicitly whether it is quoted or not
	 * (although note that 'globally quoted identifiers' setting can still cause returned Identifiers to be quoted
	 * even if {@code false} is passed in here).
	 *
	 * @param text The name
	 * @param quoted Is the identifier to be quoted explicitly.
	 *
	 * @return The identifier form of the name.
	 */
	public Identifier toIdentifier(String text, boolean quoted);

	public String toMetaDataCatalogName(Identifier identifier);
	public String toMetaDataSchemaName(Identifier identifier);
	public String toMetaDataObjectName(Identifier identifier);
	public Identifier fromMetaDataCatalogName(String catalogName);
	public Identifier fromMetaDataSchemaName(String schemaName);
	public Identifier fromMetaDataObjectName(String objectName);
}
