/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.function.Supplier;

import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.boot.models.bind.internal.BindingHelper;
import org.hibernate.boot.models.bind.internal.sources.ColumnSource;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;

import static org.hibernate.internal.util.NullnessHelper.nullif;

/// Creates mapping-model columns from source column annotations.
///
/// Column binding is deliberately small and reusable because columns are created
/// in many roles: basic attributes, identifier parts, collection values, map
/// keys, discriminator values, and dependent keys.  The caller supplies the
/// role-specific default name and defaults for uniqueness, nullability, length,
/// precision, and scale.
///
/// @since 9.0
/// @author Steve Ebersole
public class ColumnBinder {
	public static Column bindColumn(
			ColumnSource columnSource,
			Supplier<String> defaultNameSupplier) {
		return bindColumn(
				columnSource,
				defaultNameSupplier,
				false,
				true,
				255,
				0,
				0
		);
	}

	public static Column bindColumn(
			ColumnSource columnSource,
			Supplier<String> defaultNameSupplier,
			boolean uniqueByDefault,
			boolean nullableByDefault) {
		return bindColumn(
				columnSource,
				defaultNameSupplier,
				uniqueByDefault,
				nullableByDefault,
				255,
				0,
				0
		);
	}

	public static Column bindColumn(
			ColumnSource columnSource,
			Supplier<String> defaultNameSupplier,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			int lengthByDefault,
			int precisionByDefault,
			int scaleByDefault) {
		final Column result = new Column();
		result.setName( columnName( columnSource, defaultNameSupplier ) );

		result.setUnique( columnSource == null ? uniqueByDefault : columnSource.unique( uniqueByDefault ) );
		result.setNullable( columnSource == null ? nullableByDefault : columnSource.nullable( nullableByDefault ) );
		result.setSqlType( columnSource == null ? null : StringHelper.nullIfEmpty( columnSource.columnDefinition() ) );
		result.setLength( columnSource == null ? lengthByDefault : columnSource.length( lengthByDefault ) );
		result.setPrecision( columnSource == null ? precisionByDefault : columnSource.precision( precisionByDefault ) );
		result.setScale( columnSource == null ? scaleByDefault : columnSource.scale( scaleByDefault ) );
		return result;
	}


	public static String columnName(
			ColumnSource columnSource,
			Supplier<String> defaultNameSupplier) {
		if ( columnSource == null ) {
			return defaultNameSupplier.get();
		}

		return nullif( columnSource.nonEmptyName(), defaultNameSupplier );
	}

	private ColumnBinder() {
	}

	static DiscriminatorType bindDiscriminatorColumn(
			BindingContext bindingContext,
			DiscriminatorFormula formulaAnn,
			BasicValue value,
			DiscriminatorColumn columnAnn,
			BindingOptions bindingOptions,
			BindingState bindingState) {
		final ColumnSource columnSource = ColumnSource.from( columnAnn );
		final DiscriminatorType discriminatorType;
		if ( formulaAnn != null ) {
			final Formula formula = new Formula( formulaAnn.value() );
			value.addFormula( formula );

			discriminatorType = formulaAnn.discriminatorType();
		}
		else {
			final Column column = new Column();
			value.addColumn( column, true, false );
			discriminatorType = columnAnn == null ? DiscriminatorType.STRING : columnAnn.discriminatorType();

			column.setName( columnName( columnSource, () -> "dtype" ) );
			column.setLength( columnSource == null ? 31 : columnSource.length( 31 ) );
			final String columnDefinition = columnSource == null ? null : columnSource.columnDefinition();
			column.setSqlType( StringHelper.isEmpty( columnDefinition )
					? null
					: BindingHelper.applyGlobalQuoting(
							columnDefinition,
							org.hibernate.boot.models.bind.spi.QuotedIdentifierTarget.COLUMN_DEFINITION,
							bindingOptions,
							bindingState
					) );
			applyOptions( column, columnSource );
			value.getTable().addColumn( column );
		}
		return discriminatorType;
	}

	private static void applyOptions(Column column, ColumnSource columnSource) {
		if ( columnSource != null ) {
			final String options = columnSource.options();
			if ( StringHelper.isNotEmpty( options ) ) {
				// todo : see https://hibernate.atlassian.net/browse/HHH-17449
//				table.setOptions( options );
				throw new UnsupportedOperationException( "Not yet implemented" );
			}
		}
	}
}
