/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
