/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.function.Supplier;

import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.mapping.internal.context.BindingHelper;
import org.hibernate.boot.mapping.internal.sources.ColumnSource;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.relational.QuotedIdentifierTarget;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Table;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;

import static org.hibernate.internal.util.NullnessHelper.nullif;
import static org.hibernate.boot.model.internal.AnnotatedDiscriminatorColumn.DEFAULT_DISCRIMINATOR_COLUMN_NAME;

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
		return bindColumn(
				columnSource,
				defaultNameSupplier,
				uniqueByDefault,
				nullableByDefault,
				lengthByDefault,
				precisionByDefault,
				scaleByDefault,
				null,
				null
		);
	}

	public static Column bindColumn(
			ColumnSource columnSource,
			Supplier<String> defaultNameSupplier,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			BindingOptions bindingOptions,
			BindingState bindingState) {
		return bindColumn(
				columnSource,
				defaultNameSupplier,
				uniqueByDefault,
				nullableByDefault,
				255,
				0,
				0,
				bindingOptions,
				bindingState
		);
	}

	public static Column bindColumn(
			ColumnSource columnSource,
			Supplier<String> defaultNameSupplier,
			boolean uniqueByDefault,
			boolean nullableByDefault,
			int lengthByDefault,
			int precisionByDefault,
			int scaleByDefault,
			BindingOptions bindingOptions,
			BindingState bindingState) {
		final boolean explicitName = columnSource != null && columnSource.nonEmptyName() != null;
		final Column result = new Column();
		result.setExplicit( explicitName );
		result.setName( columnName( columnSource, defaultNameSupplier, bindingOptions, bindingState ) );

		result.setUnique( columnSource == null ? uniqueByDefault : columnSource.unique( uniqueByDefault ) );
		result.setNullable( columnSource == null ? nullableByDefault : columnSource.nullable( nullableByDefault ) );
		result.setSqlType( columnDefinition( columnSource, bindingOptions, bindingState ) );
		result.setLength( columnSource == null ? lengthByDefault : columnSource.length( lengthByDefault ) );
		final int precision = columnSource == null ? precisionByDefault : columnSource.precision( precisionByDefault );
		result.setPrecision( precision > 0 ? precision : null );
		final int scale = columnSource == null ? scaleByDefault : columnSource.scale( scaleByDefault );
		result.setScale( precision > 0 ? scale : null );
		applyCheckConstraints( result, columnSource );
		applyComment( result, columnSource );
		applyOptions( result, columnSource );
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

	private static String columnName(
			ColumnSource columnSource,
			Supplier<String> defaultNameSupplier,
			BindingOptions bindingOptions,
			BindingState bindingState) {
		final String name = columnName( columnSource, defaultNameSupplier );
		return finalizeColumnName(
				name,
				columnSource != null && columnSource.nonEmptyName() != null,
				bindingOptions,
				bindingState
		);
	}

	public static String finalizeColumnName(
			String logicalName,
			BindingOptions bindingOptions,
			BindingState bindingState) {
		return finalizeColumnName( logicalName, false, bindingOptions, bindingState );
	}

	private static String finalizeColumnName(
			String logicalName,
			boolean explicit,
			BindingOptions bindingOptions,
			BindingState bindingState) {
		if ( bindingOptions == null || bindingState == null ) {
			return logicalName;
		}
		final var database = bindingState.getDatabase();
		final Identifier identifier = BindingHelper.toIdentifier(
				logicalName,
				QuotedIdentifierTarget.COLUMN_NAME,
				bindingOptions,
				database.getJdbcEnvironment(),
				explicit
		);
		return bindingState.getMetadataBuildingContext()
				.getBuildingOptions()
				.getPhysicalNamingStrategy()
				.toPhysicalColumnName( identifier, database.getJdbcEnvironment() )
				.render( database.getDialect() );
	}

	public static void registerColumnNameBinding(
			Table table,
			String logicalName,
			Column column,
			BindingOptions bindingOptions,
			BindingState bindingState) {
		if ( table == null || bindingOptions == null || bindingState == null ) {
			return;
		}
		final Identifier logicalIdentifier = BindingHelper.toIdentifier(
				logicalName,
				QuotedIdentifierTarget.COLUMN_NAME,
				bindingOptions,
				bindingState.getDatabase().getJdbcEnvironment(),
				column.isExplicit()
		);
		bindingState.getRelationalModelCorrespondences()
				.columnNames()
				.register( table, logicalIdentifier, column );
		bindingState.getMetadataBuildingContext()
				.getMetadataCollector()
				.addColumnNameBinding(
						table,
						logicalIdentifier,
						column
				);
	}

	private ColumnBinder() {
	}

	private static String columnDefinition(
			ColumnSource columnSource,
			BindingOptions bindingOptions,
			BindingState bindingState) {
		if ( columnSource == null ) {
			return null;
		}
		final String columnDefinition = StringHelper.nullIfEmpty( columnSource.columnDefinition() );
		if ( columnDefinition == null ) {
			return null;
		}
		return bindingOptions == null || bindingState == null
				? columnDefinition
				: BindingHelper.applyGlobalQuoting(
						columnDefinition,
						org.hibernate.boot.mapping.internal.relational.QuotedIdentifierTarget.COLUMN_DEFINITION,
						bindingOptions,
						bindingState
				);
	}

	private static void applyCheckConstraints(Column column, ColumnSource columnSource) {
		if ( columnSource == null ) {
			return;
		}

		final jakarta.persistence.CheckConstraint[] checkConstraints = columnSource.checkConstraints();
		if ( checkConstraints == null ) {
			return;
		}

		for ( jakarta.persistence.CheckConstraint checkConstraint : checkConstraints ) {
			if ( StringHelper.isEmpty( checkConstraint.constraint() ) ) {
				continue;
			}
			column.addCheckConstraint( new org.hibernate.mapping.CheckConstraint(
					StringHelper.nullIfEmpty( checkConstraint.name() ),
					checkConstraint.constraint(),
					StringHelper.nullIfEmpty( checkConstraint.options() )
			) );
		}
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

			discriminatorType = formulaAnn.discriminatorType() == DiscriminatorType.STRING && columnAnn != null
					? columnAnn.discriminatorType()
					: formulaAnn.discriminatorType();
		}
		else {
			final Column column = new Column();
			value.addColumn( column, true, false );
			discriminatorType = columnAnn == null ? DiscriminatorType.STRING : columnAnn.discriminatorType();

			// JPA specifies DTYPE as the implicit discriminator column name;
			// see HHH-20613 before routing this through ImplicitNamingStrategy.
			column.setName( columnName( columnSource, () -> DEFAULT_DISCRIMINATOR_COLUMN_NAME ) );
			column.setLength( discriminatorType == DiscriminatorType.CHAR ? 1 : columnSource == null ? 31 : columnSource.length( 31 ) );
			final String columnDefinition = columnSource == null ? null : columnSource.columnDefinition();
			column.setSqlType( StringHelper.isEmpty( columnDefinition )
					? null
					: BindingHelper.applyGlobalQuoting(
							columnDefinition,
							org.hibernate.boot.mapping.internal.relational.QuotedIdentifierTarget.COLUMN_DEFINITION,
							bindingOptions,
							bindingState
					) );
			applyOptions( column, columnSource );
			applyComment( column, columnSource );
			value.getTable().addColumn( column );
		}
		return discriminatorType;
	}

	private static void applyComment(Column column, ColumnSource columnSource) {
		if ( columnSource != null ) {
			final String comment = columnSource.comment();
			if ( StringHelper.isNotEmpty( comment ) ) {
				column.setComment( comment );
			}
		}
	}

	private static void applyOptions(Column column, ColumnSource columnSource) {
		if ( columnSource != null ) {
			final String options = columnSource.options();
			if ( StringHelper.isNotEmpty( options ) ) {
				column.setOptions( options );
			}
		}
	}
}
