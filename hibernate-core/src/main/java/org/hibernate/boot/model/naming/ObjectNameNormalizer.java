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
package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;

/**
 * Provides centralized normalization of how database object names are handled.
 *
 * @author Steve Ebersole
 */
public abstract class ObjectNameNormalizer {
	private Database database;

	/**
	 * Normalizes the quoting of identifiers.
	 * <p/>
	 * This implements the rules set forth in JPA 2 (section "2.13 Naming of Database Objects") which
	 * states that the double-quote (") is the character which should be used to denote a <tt>quoted
	 * identifier</tt>.  Here, we handle recognizing that and converting it to the more elegant
	 * bactick (`) approach used in Hibernate..  Additionally we account for applying what JPA2 terms
	 * "globally quoted identifiers".
	 *
	 * @param identifierText The identifier to be quoting-normalized.
	 * @return The identifier accounting for any quoting that need be applied.
	 */
	public Identifier normalizeIdentifierQuoting(String identifierText) {
		return database().toIdentifier( identifierText );
	}

	protected Database database() {
		if ( database == null ) {
			database = getBuildingContext().getMetadataCollector().getDatabase();
		}
		return database;
	}

	public Identifier normalizeIdentifierQuoting(Identifier identifier) {
		return getBuildingContext().getMetadataCollector()
				.getDatabase()
				.getJdbcEnvironment()
				.getIdentifierHelper()
				.normalizeQuoting( identifier );
	}

	/**
	 * Normalizes the quoting of identifiers.  This form returns a String rather than an Identifier
	 * to better work with the legacy code in {@link org.hibernate.mapping}
	 *
	 * @param identifierText The identifier to be quoting-normalized.
	 * @return The identifier accounting for any quoting that need be applied.
	 */
	public String normalizeIdentifierQuotingAsString(String identifierText) {
		final Identifier identifier = normalizeIdentifierQuoting( identifierText );
		if ( identifier == null ) {
			return null;
		}
		return identifier.render( database().getDialect() );
	}

	public String toDatabaseIdentifierText(String identifierText) {
		return database().getDialect().quote( normalizeIdentifierQuotingAsString( identifierText ) );
	}

	/**
	 * Determine the logical name give a (potentially {@code null}/empty) explicit name.
	 *
	 * @param explicitName The explicit, user-supplied name
	 * @param namingStrategyHelper The naming strategy helper.
	 *
	 * @return The logical name
	 */
	public Identifier determineLogicalName(String explicitName, NamingStrategyHelper namingStrategyHelper) {
		Identifier logicalName;
		if ( StringHelper.isEmpty( explicitName ) ) {
			logicalName = namingStrategyHelper.determineImplicitName( getBuildingContext() );
		}
		else {
			logicalName = namingStrategyHelper.handleExplicitName( explicitName, getBuildingContext() );
		}
		logicalName = getBuildingContext().getMetadataCollector()
				.getDatabase()
				.getJdbcEnvironment()
				.getIdentifierHelper()
				.normalizeQuoting( logicalName );

		return logicalName;
	}

	/**
	 * Access the contextual information related to the current process of building metadata.  Here,
	 * that typically might be needed for accessing:<ul>
	 *     <li>{@link org.hibernate.boot.model.naming.ImplicitNamingStrategy}</li>
	 *     <li>{@link org.hibernate.boot.model.naming.PhysicalNamingStrategy}</li>
	 *     <li>{@link org.hibernate.boot.model.relational.Database}</li>
	 * </ul>
	 *
	 * @return The current building context
	 */
	protected abstract MetadataBuildingContext getBuildingContext();
}
