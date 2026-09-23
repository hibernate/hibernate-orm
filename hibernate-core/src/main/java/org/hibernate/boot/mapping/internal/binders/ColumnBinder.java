/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.model.naming.internal.ColumnNameHelper;

import org.hibernate.boot.model.naming.internal.PhysicalNamingStrategyHelper;

import java.util.function.Supplier;

import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
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
import org.hibernate.mapping.ColumnContainer;

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
	public static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "DTYPE";

	/// Preserve existing roles which do not invoke physical naming, with explicit finalization services.
	public static Column bindUntransformedColumn(ColumnSource source, Supplier<String> defaultName,
			boolean unique, boolean nullable, int length, int precision, int scale,
			org.hibernate.boot.model.relational.Database database) {
		return bindColumn( source, ColumnNameHelper.physicalName(
				database.toIdentifier( columnName( source, defaultName ) ), database ),
				unique, nullable, length, precision, scale );
	}

	/// Resolve one logical name, materialize its physical column, and retain the
	/// logical binding without invoking implicit naming again during registration.
	public static Column bindColumnWithNameBinding(
			org.hibernate.mapping.ColumnContainer table,
			ColumnSource source, Supplier<String> implicitName,
			boolean unique, boolean nullable, int length, int precision, int scale,
			BindingOptions options, BindingState state) {
		final LogicalName logicalName = logicalColumnName( source, implicitName );
		final Column column = bindColumn( source, () -> logicalName.toString(),
				unique, nullable, length, precision, scale, options, state );
		registerColumnNameBinding( table, logicalName, column, options, state );
		return column;
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
		final var column = bindColumnMetadata( columnSource, columnName( columnSource, defaultNameSupplier, bindingOptions, bindingState ),
				uniqueByDefault, nullableByDefault, lengthByDefault, precisionByDefault, scaleByDefault );
		column.setSqlType( columnDefinition( columnSource, bindingOptions, bindingState ) );
		return column;
	}

	public static Column bindColumn(ColumnSource columnSource, PhysicalName name,
			boolean uniqueByDefault, boolean nullableByDefault,
			int lengthByDefault, int precisionByDefault, int scaleByDefault) {
		final var result = bindColumnMetadata( columnSource, name, uniqueByDefault, nullableByDefault,
				lengthByDefault, precisionByDefault, scaleByDefault );
		result.setSqlType( columnSource == null ? null : StringHelper.nullIfEmpty( columnSource.columnDefinition() ) );
		return result;
	}

	private static Column bindColumnMetadata(ColumnSource columnSource, PhysicalName name,
			boolean uniqueByDefault, boolean nullableByDefault,
			int lengthByDefault, int precisionByDefault, int scaleByDefault) {
		final Column result = new Column( name );

		result.setUnique( columnSource == null ? uniqueByDefault : columnSource.unique( uniqueByDefault ) );
		result.setNullable( columnSource == null ? nullableByDefault : columnSource.nullable( nullableByDefault ) );
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

	private static PhysicalName columnName(
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

	public static PhysicalName finalizeColumnName(
			String logicalName,
			BindingOptions bindingOptions,
			BindingState bindingState) {
		return finalizeColumnName( logicalName, false, bindingOptions, bindingState );
	}

	public static PhysicalName finalizeColumnName(
			String logicalName,
			boolean explicit,
			BindingOptions bindingOptions,
			BindingState bindingState) {
		final var database = bindingState.getDatabase();
		final Identifier identifier = BindingHelper.toIdentifier(
				logicalName,
				QuotedIdentifierTarget.COLUMN_NAME,
				bindingOptions,
				database.getJdbcEnvironment(),
				explicit
		);
		return PhysicalNamingStrategyHelper.resolve(
				PhysicalNamingStrategyHelper.logicalName( identifier ), database.getJdbcEnvironment(),
				bindingState.getMetadataBuildingContext().getBuildingPlan().getPhysicalNamingStrategy()::toPhysicalColumnName,
				"column", false );
	}

	public static LogicalName logicalColumnName(ColumnSource source, Supplier<String> defaultName) {
		final var identifier = Identifier.toIdentifier( columnName( source, defaultName ), false, false );
		return new LogicalName( identifier.getText(), identifier.isQuoted(), source != null && source.nonEmptyName() != null );
	}

	public static void registerColumnNameBinding(
			ColumnContainer table,
			LogicalName logicalName,
			Column column,
			BindingOptions bindingOptions,
			BindingState bindingState) {
		if ( table == null || bindingOptions == null || bindingState == null ) {
			return;
		}
		registerColumnNameBinding( table, logicalName, column, bindingState );
	}

	public static void registerColumnNameBinding(
			ColumnContainer table,
			LogicalName logicalName,
			Column column,
			BindingState bindingState) {
		if ( table == null || bindingState == null ) {
			return;
		}
		final var logicalIdentifier = logicalName;
		bindingState.getRelationalModelCorrespondences()
				.columnNames()
				.register( table, logicalIdentifier, column );
		if ( !(table instanceof Table relationalTable) ) {
			return;
		}
		bindingState.getMetadataBuildingContext()
				.getMetadataCollector()
				.addColumnNameBinding(
						relationalTable,
						logicalIdentifier == null ? null : new Identifier(
								logicalIdentifier.getText(), logicalIdentifier.isQuoted(), logicalIdentifier.isExplicit() ),
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
			BindingState bindingState,
			Supplier<String> implicitName) {
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
			final LogicalName logicalName = logicalColumnName( columnSource, implicitName );
			final Column column = new Column( finalizeColumnName(
					logicalName.toString(), logicalName.isExplicit(), bindingOptions, bindingState ) );
			registerColumnNameBinding( value.getColumnContainer(), logicalName, column, bindingOptions, bindingState );
			value.addColumn( column, true, false );
			discriminatorType = columnAnn == null ? DiscriminatorType.STRING : columnAnn.discriminatorType();

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
			value.getColumnContainer().addColumn( column );
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
