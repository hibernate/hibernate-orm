/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SingleTableSubclass;

import static org.hibernate.persister.entity.DiscriminatorHelper.getDiscriminatorSQLValue;

/// Finalizes physical constraints for an entity after value and key binding.
///
/// This replaces the new-pipeline use of anonymous metadata-collector second
/// passes and mapping-object callbacks for late entity constraint creation.
///
/// @since 9.0
/// @author Steve Ebersole
final class EntityConstraintFinalizer {
	private EntityConstraintFinalizer() {
	}

	static void finalizeConstraints(PersistentClass entityBinding, MetadataBuildingContext context) {
		if ( entityBinding instanceof SingleTableSubclass subclass ) {
			finalizeSingleTableSubclassConstraints( subclass, context );
		}
	}

	private static void finalizeSingleTableSubclassConstraints(
			SingleTableSubclass subclass,
			MetadataBuildingContext context) {
		if ( subclass.isAbstract() ) {
			return;
		}

		final var dialect = context.getMetadataCollector().getDatabase().getDialect();
		if ( !dialect.supportsTableCheck() ) {
			return;
		}

		final var discriminator = subclass.getDiscriminator();
		final var selectables = discriminator.getSelectables();
		if ( selectables.size() != 1 ) {
			return;
		}

		final var check = new StringBuilder();
		check.append( selectables.get( 0 ).getText( dialect ) );
		if ( subclass.isDiscriminatorValueNull() ) {
			check.append( " is " );
		}
		else if ( subclass.isDiscriminatorValueNotNull() ) {
			// Can't enforce this for now, because "not null" really means
			// "not null and not any of the other explicitly listed values".
			return;
		}
		else {
			check.append( " <> " );
		}

		check.append( getDiscriminatorSQLValue( subclass, dialect ) )
				.append( " or (" );
		boolean first = true;
		for ( var property : subclass.getSuperclass().getUnjoinedProperties() ) {
			first = appendNullableColumnChecks( property, check, first, dialect );
		}
		for ( var property : subclass.getUnjoinedProperties() ) {
			first = appendNullableColumnChecks( property, check, first, dialect );
		}
		check.append( ")" );
		if ( !first ) {
			subclass.getTable().addCheck( new CheckConstraint( check.toString() ) );
		}
	}

	private static boolean appendNullableColumnChecks(
			Property property,
			StringBuilder check,
			boolean first,
			Dialect dialect) {
		if ( !property.isComposite() && !property.isOptional() ) {
			for ( var selectable : property.getSelectables() ) {
				if ( selectable instanceof Column column && column.isNullable() ) {
					if ( first ) {
						first = false;
					}
					else {
						check.append( " and " );
					}
					check.append( column.getQuotedName( dialect ) )
							.append( " is not null" );
				}
			}
		}
		return first;
	}
}
