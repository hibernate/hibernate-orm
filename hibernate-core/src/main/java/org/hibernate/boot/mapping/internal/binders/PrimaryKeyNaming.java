/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.MappingException;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.model.naming.internal.ImplicitNamingContextImpl;
import org.hibernate.boot.model.naming.internal.PhysicalNamingStrategyHelper;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.boot.model.naming.spi.NamingNamePair;
import org.hibernate.boot.model.naming.spi.PrimaryKeyNamingInput;
import org.hibernate.mapping.NamedTable;

/// Finalizes names once for surviving primary keys after model binding.
///
/// @author Steve Ebersole
final class PrimaryKeyNaming {
	private PrimaryKeyNaming() {}

	static void finish(BindingState state) {
		final var context = state.getMetadataBuildingContext();
		final var database = context.getMetadataCollector().getDatabase();
		final var implicitContext = ImplicitNamingContextImpl.from( context );
		final var implicitStrategy = context.getBuildingPlan().getImplicitNamingStrategy();
		final var physicalStrategy = context.getBuildingPlan().getPhysicalNamingStrategy();
		for ( var table : context.getMetadataCollector().collectTableMappings() ) {
			final var key = table.getPrimaryKey();
			if ( !(table instanceof NamedTable namedTable) || key == null
					|| key.getTable() != table || key.getName() != null ) {
				continue;
			}
			final var reference = state.getTableByBinding( table );
			if ( reference == null ) {
				// Auxiliary and generator tables have separate naming lifecycles.
				continue;
			}
			// todo : (HHH-20914) resolve explicit @PrimaryKey declarations here, bypassing implicit naming.
			final var input = new PrimaryKeyNamingInput( new NamedTableNamingInput(
					new NamingNamePair( reference.logicalName(), namedTable.getPhysicalName().objectName() )
			) );
			final var logical = implicitStrategy.determinePrimaryKeyName( input, implicitContext );
			//noinspection ConstantValue
			if ( logical == null ) {
				throw new MappingException( "Implicit naming strategy must return a non-null name for primary key" );
			}
			final var physical = PhysicalNamingStrategyHelper.resolve(
					logical,
					database.getJdbcEnvironment(),
					physicalStrategy::toPhysicalPrimaryKeyName,
					"primary key",
					false
			);
			assert physical != null;
			key.setName( PhysicalNamingStrategyHelper.physicalIdentifier( physical ).render( database.getDialect() ) );
		}
	}
}
