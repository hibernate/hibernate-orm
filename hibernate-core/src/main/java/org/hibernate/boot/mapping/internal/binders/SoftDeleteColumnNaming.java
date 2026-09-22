/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.Optional;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.internal.ImplicitNamingHelper;
import org.hibernate.boot.model.naming.internal.PhysicalNamingStrategyHelper;
import org.hibernate.boot.model.naming.spi.SoftDeleteColumnNamingInput;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.relational.naming.spi.LogicalName;

/// Shared name resolution for entity and collection soft-delete indicators.
/// Value construction and resolution remain in the respective binders.
///
/// @author Steve Ebersole
final class SoftDeleteColumnNaming {
	private SoftDeleteColumnNaming() {}

	static Column column(SoftDelete config, PersistentClass owner, Optional<String> path,
			Table table, BindingState state) {
		final var context = state.getMetadataBuildingContext();
		final boolean explicit = !config.columnName().isEmpty();
		final String name = explicit ? config.columnName() : ImplicitNamingHelper.columnName(
				context.getBuildingPlan().getImplicitNamingStrategy().determineSoftDeleteColumnName(
						new SoftDeleteColumnNamingInput( JoinColumnNaming.entity( owner ),
								path.isPresent() ? SoftDeleteColumnNamingInput.Kind.COLLECTION : SoftDeleteColumnNamingInput.Kind.ENTITY,
								path, JoinColumnNaming.table( table, owner, state ), config.strategy() ),
						JoinColumnNaming.context( state ) ), "soft-delete column" );
		final var identifier = Identifier.toIdentifier( name, false, false );
		final var logicalName = identifier == null ? null
				: new LogicalName( identifier.getText(), identifier.isQuoted(), explicit );
		final var column = new Column( PhysicalNamingStrategyHelper.resolve(
				logicalName, state.getDatabase().getJdbcEnvironment(),
				context.getBuildingPlan().getPhysicalNamingStrategy()::toPhysicalColumnName, "column", false ) );
		ColumnBinder.registerColumnNameBinding( table, logicalName, column, state );
		return column;
	}
}
