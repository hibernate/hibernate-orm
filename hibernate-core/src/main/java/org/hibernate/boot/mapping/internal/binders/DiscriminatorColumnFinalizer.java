/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.type.MappingContext;

import static org.hibernate.persister.entity.DiscriminatorHelper.getDiscriminatorValue;

/// Finalizes discriminator column metadata after entity hierarchy binding.
///
/// This replaces the new-pipeline use of the legacy discriminator-column second
/// pass by applying nullable discriminator handling and dialect check-constraint
/// generation as an explicit binding finalization step.
///
/// @since 9.0
/// @author Steve Ebersole
final class DiscriminatorColumnFinalizer {
	private DiscriminatorColumnFinalizer() {
	}

	static void finalizeDiscriminatorColumn(RootClass rootClass, Dialect dialect, MappingContext mappingContext) {
		if ( hasNullDiscriminatorValue( rootClass ) ) {
			for ( var selectable : rootClass.getDiscriminator().getSelectables() ) {
				if ( selectable instanceof Column column ) {
					column.setNullable( true );
				}
			}
		}
		if ( !hasNotNullDiscriminatorValue( rootClass )
				&& !rootClass.getDiscriminator().hasFormula()
				&& !rootClass.isForceDiscriminator() ) {
			final var column = rootClass.getDiscriminator().getColumns().get( 0 );
			column.addCheckConstraint( new CheckConstraint( checkConstraint( rootClass, column, dialect, mappingContext ) ) );
		}
	}

	private static boolean hasNullDiscriminatorValue(PersistentClass rootClass) {
		if ( rootClass.isDiscriminatorValueNull() ) {
			return true;
		}
		for ( var subclass : rootClass.getSubclasses() ) {
			if ( subclass.isDiscriminatorValueNull() ) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasNotNullDiscriminatorValue(PersistentClass rootClass) {
		if ( rootClass.isDiscriminatorValueNotNull() ) {
			return true;
		}
		for ( var subclass : rootClass.getSubclasses() ) {
			if ( subclass.isDiscriminatorValueNotNull() ) {
				return true;
			}
		}
		return false;
	}

	private static String checkConstraint(RootClass rootClass, Column column, Dialect dialect, MappingContext mappingContext) {
		return dialect.getCheckCondition(
				column.getQuotedName( dialect ),
				discriminatorValues( rootClass ),
				column.getType( mappingContext ).getJdbcType()
		);
	}

	private static List<String> discriminatorValues(RootClass rootClass) {
		final List<String> values = new ArrayList<>();
		if ( !rootClass.isAbstract()
				&& !rootClass.isDiscriminatorValueNull()
				&& !rootClass.isDiscriminatorValueNotNull() ) {
			values.add( getDiscriminatorValue( rootClass ).toString() );
		}
		for ( Subclass subclass : rootClass.getSubclasses() ) {
			if ( !subclass.isAbstract()
					&& !subclass.isDiscriminatorValueNull()
					&& !subclass.isDiscriminatorValueNotNull() ) {
				values.add( getDiscriminatorValue( subclass ).toString() );
			}
		}
		return values;
	}
}
