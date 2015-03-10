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
package org.hibernate.boot.model.naming;

import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * Helper contract for dealing with naming strategies.
 */
public interface NamingStrategyHelper {
	/**
	 * Called when the user supplied no explicit name/identifier for the given database object.
	 * <p/>
	 * Typically implementations will access the {@link ImplicitNamingStrategy} via
	 * {@link org.hibernate.boot.spi.MetadataBuildingContext#getBuildingOptions()} ->
	 * {@link org.hibernate.boot.spi.MetadataBuildingOptions#getImplicitNamingStrategy()}
	 * <p/>
	 * For proper quoting, {@link org.hibernate.boot.model.relational.Database#toIdentifier(String)}
	 * should be used via
	 * {@link org.hibernate.boot.spi.MetadataBuildingContext#getMetadataCollector()} ->
	 * {@link org.hibernate.boot.spi.InFlightMetadataCollector#getDatabase()}
	 *
	 * @param buildingContext The building context in which this is called.
	 *
	 * @return The implicit name
	 */
	public Identifier determineImplicitName(MetadataBuildingContext buildingContext);

	/**
	 * Called when the user has supplied an explicit name for the database object.
	 * <p/>
	 * Typically implementations will access the {@link ImplicitNamingStrategy} via
	 * {@link org.hibernate.boot.spi.MetadataBuildingContext#getBuildingOptions()} ->
	 * {@link org.hibernate.boot.spi.MetadataBuildingOptions#getImplicitNamingStrategy()}
	 * <p/>
	 * For proper quoting, {@link org.hibernate.boot.model.relational.Database#toIdentifier(String)}
	 * should be used via
	 * {@link org.hibernate.boot.spi.MetadataBuildingContext#getMetadataCollector()} ->
	 * {@link org.hibernate.boot.spi.InFlightMetadataCollector#getDatabase()}
	 *
	 * @param explicitName The explicit object name.
	 * @param buildingContext The building context in which this is called.
	 *
	 * @return The strategy-handled name.
	 */
	public Identifier handleExplicitName(String explicitName, MetadataBuildingContext buildingContext);

	/**
	 * Handle converting a logical name to a physical name
	 * <p/>
	 * Typically implementations will access the {@link PhysicalNamingStrategy} via
	 * {@link org.hibernate.boot.spi.MetadataBuildingContext#getBuildingOptions()} ->
	 * {@link org.hibernate.boot.spi.MetadataBuildingOptions#getPhysicalNamingStrategy()}
	 * <p/>
	 * For proper quoting, {@link org.hibernate.boot.model.relational.Database#toIdentifier(String)}
	 * should be used via
	 * {@link org.hibernate.boot.spi.MetadataBuildingContext#getMetadataCollector()} ->
	 * {@link org.hibernate.boot.spi.InFlightMetadataCollector#getDatabase()}
	 *
	 * @param logicalName The logical name to convert to a physical name
	 * @param buildingContext The building context in which this is called.
	 *
	 * @return The physical name
	 */
	public Identifier toPhysicalName(Identifier logicalName, MetadataBuildingContext buildingContext);
}
