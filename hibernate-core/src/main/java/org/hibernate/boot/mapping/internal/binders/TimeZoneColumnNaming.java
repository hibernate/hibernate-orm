/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.model.naming.internal.ImplicitNamingHelper;
import org.hibernate.boot.model.naming.spi.BasicColumnNamingInput;
import org.hibernate.boot.model.naming.spi.NamingNamePair;
import org.hibernate.boot.model.naming.spi.TimeZoneColumnNamingInput;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ColumnContainer;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.usertype.internal.OffsetDateTimeCompositeUserType;
import org.hibernate.usertype.internal.OffsetTimeCompositeUserType;
import org.hibernate.usertype.internal.ZonedDateTimeCompositeUserType;

import static org.hibernate.usertype.internal.AbstractTimeZoneStorageCompositeUserType.ZONE_OFFSET_NAME;

/// Per-use dependency state: bind the temporal column before naming its companion.
/// No names or mutable resolution state are cached on reusable mapping sources.
///
/// @author Steve Ebersole
final class TimeZoneColumnNaming {
	private final ComponentSource source;
	private final PersistentClass owner;
	private final BindingState state;
	private NamingNamePair temporalName;
	private LogicalName implicitTemporalName;

	private TimeZoneColumnNaming(ComponentSource source, PersistentClass owner, BindingState state) {
		this.source = source;
		this.owner = owner;
		this.state = state;
	}

	static TimeZoneColumnNaming forComponent(ComponentSource source, PersistentClass owner, BindingState state) {
		if ( source.sourceMember() == null || !org.hibernate.boot.model.internal.TimeZoneStorageHelper.useColumnForTimeZoneStorage(
				source.sourceMember(), state.getMetadataBuildingContext() ) ) {
			return null;
		}
		final String memberType = source.memberSourceType().getName();
		return List.of(
				OffsetDateTimeCompositeUserType.OffsetDateTimeEmbeddable.class.getName(),
				OffsetTimeCompositeUserType.OffsetTimeEmbeddable.class.getName(),
				ZonedDateTimeCompositeUserType.ZonedDateTimeEmbeddable.class.getName() ).contains( memberType )
				? new TimeZoneColumnNaming( source, owner, state ) : null;
	}

	static boolean isCompanion(String attributeName) {
		return ZONE_OFFSET_NAME.equals( attributeName );
	}

	Supplier<String> implicitName(String attributeName, String tableName, ColumnContainer defaultTable) {
		return ImplicitNamingHelper.once( () -> {
			final var strategy = state.getMetadataBuildingContext().getBuildingPlan().getImplicitNamingStrategy();
			final var context = JoinColumnNaming.context( state );
			if ( !isCompanion( attributeName ) ) {
				implicitTemporalName = strategy.determineBasicColumnName( new BasicColumnNamingInput( source.timeZoneStorageBasePath() ), context );
				return implicitTemporalName;
			}
			if ( temporalName == null ) {
				throw new MappingException( "Temporal column must be resolved before its time-zone companion" );
			}
			final var table = tableName == null || tableName.isEmpty()
					? defaultTable.requireTable() : owner.getTable( tableName );
			return strategy.determineTimeZoneColumnName( new TimeZoneColumnNamingInput(
					JoinColumnNaming.entity( owner ), source.timeZoneStorageBasePath(), temporalName,
					JoinColumnNaming.table( table, owner, state ), source.timeZoneColumnDeclared(), sourceColumnName() ), context );
		}, isCompanion( attributeName ) ? "time-zone column" : "basic column" );
	}

	private Optional<LogicalName> sourceColumnName() {
		final var column = source.sourceMember().getDirectAnnotationUsage( jakarta.persistence.Column.class );
		if ( column != null && !column.name().isEmpty() ) {
			final var name = Identifier.toIdentifier( column.name(), false, false );
			return Optional.of( new LogicalName( name.getText(), name.isQuoted(), true ) );
		}
		return Optional.ofNullable( implicitTemporalName );
	}

	void columnBound(String attributeName, Column column, ColumnContainer table) {
		if ( !isCompanion( attributeName ) ) {
			final var logicalName = state.getRelationalModelCorrespondences().columnNames().findDeclarationName( table, column );
			if ( logicalName == null ) {
				throw new MappingException( "Missing logical temporal column dependency: " + column.getName() );
			}
			temporalName = new NamingNamePair( logicalName, column.getPhysicalName() );
		}
	}
}
