/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.hibernate.cache.spi.access.AccessType;

/**
 * Defines a (contextual) set of values to use as defaults in the absence of related mapping information.  The
 * context here is conceptually a stack.  The "global" level is configuration settings.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 *
 * @since 5.0
 */
public interface MappingDefaults {
	public static final String DEFAULT_IDENTIFIER_COLUMN_NAME = "id";
	public static final String DEFAULT_TENANT_IDENTIFIER_COLUMN_NAME = "tenant_id";
	public static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "class";
	public static final String DEFAULT_CASCADE_NAME = "none";
	public static final String DEFAULT_PROPERTY_ACCESS_NAME = "property";
	/**
	 * Identifies the database schema name to use if none specified in the mapping.
	 *
	 * @return The implicit schema name; may be {@code null}
	 */
	public String getImplicitSchemaName();

	/**
	 * Identifies the database catalog name to use if none specified in the mapping.
	 *
	 * @return The implicit catalog name; may be {@code null}
	 */
	public String getImplicitCatalogName();

	/**
	 * Should all database identifiers encountered in this context be implicitly quoted?
	 *
	 * {@code true} indicates that all identifier encountered within this context should be
	 * quoted.  {@code false} indicates indicates that identifiers within this context are
	 * onl;y quoted if explicitly quoted.
	 *
	 * @return {@code true}/{@code false}
	 */
	public boolean shouldImplicitlyQuoteIdentifiers();

	/**
	 * Identifies the column name to use for the identifier column if none specified in
	 * the mapping.
	 *
	 * @return The implicit identifier column name
	 */
	public String getImplicitIdColumnName();

	/**
	 * Identifies the column name to use for the tenant identifier column if none is
	 * specified in the mapping.
	 *
	 * @return The implicit tenant identifier column name
	 */
	public String getImplicitTenantIdColumnName();

	/**
	 * Identifies the column name to use for the discriminator column if none specified
	 * in the mapping.
	 *
	 * @return The implicit discriminator column name
	 */
	public String getImplicitDiscriminatorColumnName();

	/**
	 * Identifies the package name to use if none specified in the mapping.  Really only
	 * pertinent for {@code hbm.xml} mappings.
	 *
	 * @return The implicit package name.
	 */
	public String getImplicitPackageName();

	/**
	 * Is auto-importing of entity (short) names enabled?
	 *
	 * @return {@code true} if auto-importing is enabled; {@code false} otherwise.
	 */
	public boolean isAutoImportEnabled();

	/**
	 * Identifies the cascade style to apply to associations if none specified in the mapping.
	 *
	 * @return The implicit cascade style
	 */
	public String getImplicitCascadeStyleName();

	/**
	 * Identifies the default {@link org.hibernate.property.access.spi.PropertyAccessStrategy} name to use if none specified in the
	 * mapping.
	 *
	 * @return The implicit property accessor name
	 *
	 * @see org.hibernate.property.access.spi.PropertyAccessStrategy
	 */
	public String getImplicitPropertyAccessorName();

	/**
	 * Identifies whether singular associations (many-to-one, one-to-one) are lazy
	 * by default if not specified in the mapping.
	 *
	 * @return The implicit association laziness
	 */
	public boolean areEntitiesImplicitlyLazy();

	/**
	 * Identifies whether plural attributes are lazy by default if not specified in the mapping.
	 *
	 * @return The implicit association laziness
	 */
	public boolean areCollectionsImplicitlyLazy();

	/**
	 * The cache access type to use if none is specified
	 *
	 * @return The implicit cache access type.
	 */
	public AccessType getImplicitCacheAccessType();
}
