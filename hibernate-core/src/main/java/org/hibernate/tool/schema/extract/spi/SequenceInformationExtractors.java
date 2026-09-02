/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.spi;

import java.util.ArrayList;
import java.util.Collections;

import org.hibernate.SPI;
import org.hibernate.boot.model.relational.QualifiedSequenceName;

import static org.hibernate.SPI.Role.USE;

/// Create immutable sequence-information extractors from lookup SQL and row
/// readers.
///
/// Use [#none()] when a database version does not expose sequence metadata. Use
/// [#builder(String)] for a single sequence-discovery query, and configure only
/// the columns or custom readers needed by that query. Supply the resulting
/// extractor from
/// [org.hibernate.dialect.Dialect#getSequenceInformationExtractor()].
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class SequenceInformationExtractors {
	private static final SequenceInformationExtractor NONE = extractionContext -> Collections.emptyList();

	private SequenceInformationExtractors() {
	}

	/// Return the stable extractor which performs no JDBC operation and reports
	/// no sequences.
	public static SequenceInformationExtractor none() {
		return NONE;
	}

	/// Begin configuring an extractor which executes `lookupSql` exactly once.
	public static Builder builder(String lookupSql) {
		return new Builder( requireText( lookupSql, "lookupSql" ) );
	}

	/// Create immutable sequence information preserving the supplied nullable
	/// numeric value references exactly.
	public static SequenceInformation information(
			QualifiedSequenceName name,
			Number startValue,
			Number minimumValue,
			Number maximumValue,
			Number incrementValue) {
		if ( name == null ) {
			throw new IllegalArgumentException( "name must not be null" );
		}
		return new StandardSequenceInformation( name, startValue, minimumValue, maximumValue, incrementValue );
	}

	private static String requireText(String value, String description) {
		if ( value == null || value.isBlank() ) {
			throw new IllegalArgumentException( description + " must not be null or blank" );
		}
		return value;
	}

	private static <T> T requireReader(T reader, String description) {
		if ( reader == null ) {
			throw new IllegalArgumentException( description + " must not be null" );
		}
		return reader;
	}

	/// Configure the row mapping used by a standard single-query extractor.
	///
	/// Each configuration method replaces the previous reader for the same
	/// value. Call [#build()] after configuration and retain the resulting
	/// immutable extractor instead of retaining this mutable builder.
	///
	/// @since 8.0
	/// @author Steve Ebersole
	@SPI(USE)
	public static final class Builder {
		private final String lookupSql;
		private SequenceMetadataValueReader<String> sequenceNameReader = row -> row.getString( "sequence_name" );
		private SequenceMetadataValueReader<String> catalogReader = row -> row.getString( "sequence_catalog" );
		private SequenceMetadataValueReader<String> schemaReader = row -> row.getString( "sequence_schema" );
		private SequenceMetadataValueReader<? extends Number> startValueReader = row -> row.getLong( "start_value" );
		private SequenceMetadataValueReader<? extends Number> minimumValueReader = row -> row.getLong( "minimum_value" );
		private SequenceMetadataValueReader<? extends Number> maximumValueReader = row -> row.getLong( "maximum_value" );
		private SequenceMetadataValueReader<? extends Number> incrementValueReader = row -> row.getLong( "increment" );

		private Builder(String lookupSql) {
			this.lookupSql = lookupSql;
		}

		/// Read the sequence name with `ResultSet#getString(label)`.
		public Builder sequenceNameColumn(String label) {
			final String column = requireText( label, "sequence name column label" );
			return sequenceNameReader( row -> row.getString( column ) );
		}

		/// Read the sequence name with `ResultSet#getString(position)`.
		public Builder sequenceNameColumn(int position) {
			if ( position < 1 ) {
				throw new IllegalArgumentException( "sequence name column position must be at least one" );
			}
			return sequenceNameReader( row -> row.getString( position ) );
		}

		/// Read the sequence name with `reader`.
		public Builder sequenceNameReader(SequenceMetadataValueReader<String> reader) {
			sequenceNameReader = requireReader( reader, "sequence name reader" );
			return this;
		}

		/// Read the catalog with `ResultSet#getString(label)`.
		public Builder catalogColumn(String label) {
			final String column = requireText( label, "catalog column label" );
			return catalogReader( row -> row.getString( column ) );
		}

		/// Read the catalog with `reader`.
		public Builder catalogReader(SequenceMetadataValueReader<String> reader) {
			catalogReader = requireReader( reader, "catalog reader" );
			return this;
		}

		/// Do not read a catalog; report it as `null`.
		public Builder withoutCatalog() {
			catalogReader = row -> null;
			return this;
		}

		/// Read the schema with `ResultSet#getString(label)`.
		public Builder schemaColumn(String label) {
			final String column = requireText( label, "schema column label" );
			return schemaReader( row -> row.getString( column ) );
		}

		/// Read the schema with `reader`.
		public Builder schemaReader(SequenceMetadataValueReader<String> reader) {
			schemaReader = requireReader( reader, "schema reader" );
			return this;
		}

		/// Do not read a schema; report it as `null`.
		public Builder withoutSchema() {
			schemaReader = row -> null;
			return this;
		}

		/// Read the start value with `ResultSet#getLong(label)`.
		public Builder startValueColumn(String label) {
			final String column = requireText( label, "start-value column label" );
			return startValueReader( row -> row.getLong( column ) );
		}

		/// Read the start value with `reader`.
		public Builder startValueReader(SequenceMetadataValueReader<? extends Number> reader) {
			startValueReader = requireReader( reader, "start-value reader" );
			return this;
		}

		/// Do not read a start value; report it as `null`.
		public Builder withoutStartValue() {
			startValueReader = row -> null;
			return this;
		}

		/// Read the minimum value with `ResultSet#getLong(label)`.
		public Builder minimumValueColumn(String label) {
			final String column = requireText( label, "minimum-value column label" );
			return minimumValueReader( row -> row.getLong( column ) );
		}

		/// Read the minimum value with `reader`.
		public Builder minimumValueReader(SequenceMetadataValueReader<? extends Number> reader) {
			minimumValueReader = requireReader( reader, "minimum-value reader" );
			return this;
		}

		/// Do not read a minimum value; report it as `null`.
		public Builder withoutMinimumValue() {
			minimumValueReader = row -> null;
			return this;
		}

		/// Read the maximum value with `ResultSet#getLong(label)`.
		public Builder maximumValueColumn(String label) {
			final String column = requireText( label, "maximum-value column label" );
			return maximumValueReader( row -> row.getLong( column ) );
		}

		/// Read the maximum value with `reader`.
		public Builder maximumValueReader(SequenceMetadataValueReader<? extends Number> reader) {
			maximumValueReader = requireReader( reader, "maximum-value reader" );
			return this;
		}

		/// Do not read a maximum value; report it as `null`.
		public Builder withoutMaximumValue() {
			maximumValueReader = row -> null;
			return this;
		}

		/// Read the increment value with `ResultSet#getLong(label)`.
		public Builder incrementValueColumn(String label) {
			final String column = requireText( label, "increment-value column label" );
			return incrementValueReader( row -> row.getLong( column ) );
		}

		/// Read the increment value with `reader`.
		public Builder incrementValueReader(SequenceMetadataValueReader<? extends Number> reader) {
			incrementValueReader = requireReader( reader, "increment-value reader" );
			return this;
		}

		/// Do not read an increment value; report it as `null`.
		public Builder withoutIncrementValue() {
			incrementValueReader = row -> null;
			return this;
		}

		/// Capture the current configuration in an immutable extractor.
		public SequenceInformationExtractor build() {
			final SequenceMetadataValueReader<String> sequenceName = sequenceNameReader;
			final SequenceMetadataValueReader<String> catalog = catalogReader;
			final SequenceMetadataValueReader<String> schema = schemaReader;
			final SequenceMetadataValueReader<? extends Number> start = startValueReader;
			final SequenceMetadataValueReader<? extends Number> minimum = minimumValueReader;
			final SequenceMetadataValueReader<? extends Number> maximum = maximumValueReader;
			final SequenceMetadataValueReader<? extends Number> increment = incrementValueReader;
			return extractionContext -> extractionContext.getQueryResults(
					lookupSql,
					null,
					resultSet -> {
						final var identifierHelper = extractionContext.getJdbcEnvironment().getIdentifierHelper();
						final var results = new ArrayList<SequenceInformation>();
						while ( resultSet.next() ) {
							results.add( information(
									new QualifiedSequenceName(
											identifierHelper.toIdentifier( catalog.read( resultSet ) ),
											identifierHelper.toIdentifier( schema.read( resultSet ) ),
											identifierHelper.toIdentifier( sequenceName.read( resultSet ) )
									),
									start.read( resultSet ),
									minimum.read( resultSet ),
									maximum.read( resultSet ),
									increment.read( resultSet )
							) );
						}
						return results;
					}
			);
		}
	}

	private record StandardSequenceInformation(
			QualifiedSequenceName sequenceName,
			Number startValue,
			Number minimumValue,
			Number maximumValue,
			Number incrementValue) implements SequenceInformation {
		@Override
		public QualifiedSequenceName getSequenceName() {
			return sequenceName;
		}

		@Override
		public Number getStartValue() {
			return startValue;
		}

		@Override
		public Number getMinValue() {
			return minimumValue;
		}

		@Override
		public Number getMaxValue() {
			return maximumValue;
		}

		@Override
		public Number getIncrementValue() {
			return incrementValue;
		}
	}
}
