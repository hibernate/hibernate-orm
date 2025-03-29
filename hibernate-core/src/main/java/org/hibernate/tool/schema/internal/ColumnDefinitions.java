/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.type.SqlTypes.isNumericOrDecimal;
import static org.hibernate.type.SqlTypes.isStringType;

class ColumnDefinitions {

	static boolean hasMatchingType(Column column, ColumnInformation columnInformation, Metadata metadata, Dialect dialect) {
		final boolean typesMatch = dialect.equivalentTypes( column.getSqlTypeCode( metadata ), columnInformation.getTypeCode() )
				|| normalize( stripArgs( column.getSqlType( metadata ) ) ).equals( normalize( columnInformation.getTypeName() ) );
		if ( typesMatch ) {
			return true;
		}
		else {
			// Try to resolve the JdbcType by type name and check for a match again based on that type code.
			// This is used to handle SqlTypes type codes like TIMESTAMP_UTC etc.
			final JdbcType jdbcType = dialect.resolveSqlTypeDescriptor(
					columnInformation.getTypeName(),
					columnInformation.getTypeCode(),
					columnInformation.getColumnSize(),
					columnInformation.getDecimalDigits(),
					metadata.getDatabase().getTypeConfiguration().getJdbcTypeRegistry()
			);
			return dialect.equivalentTypes( column.getSqlTypeCode( metadata ), jdbcType.getDefaultSqlTypeCode() );
		}
	}

	static boolean hasMatchingLength(Column column, ColumnInformation columnInformation, Metadata metadata, Dialect dialect) {
		if ( !column.getSqlType( metadata ).contains("(") ) {
			// the DDL type does not explicitly specify a length,
			// and so we do not require an exact match
			return true;
		}
		else {
			final int sqlType = columnInformation.getTypeCode();
			if ( isStringType( sqlType ) ) {
				final int actualLength = columnInformation.getColumnSize();
				final Size size = column.getColumnSize( dialect, metadata );
				final Long requiredLength = size.getLength();
				return requiredLength == null
					|| requiredLength == actualLength;
			}
			else if ( isNumericOrDecimal( sqlType ) ) {
				// Postgres, H2, SQL Server, and MySQL agree on the following:
				final int actualPrecision = columnInformation.getColumnSize();
				final int actualScale = columnInformation.getDecimalDigits();
				final Size size = column.getColumnSize( dialect, metadata );
				final Integer requiredPrecision = size.getPrecision();
				final Integer requiredScale = size.getScale();
				return requiredPrecision == null
					|| requiredScale == null
					|| requiredScale == actualScale && requiredPrecision == actualPrecision;
			}
			// I would really love this to be able to change the binary
			// precision of a float/double type, but there simply doesn't
			// seem to be any good way to implement it
			else {
				return true;
			}
		}
	}

	static String getFullColumnDeclaration(
			Column column,
			Table table,
			Metadata metadata,
			Dialect dialect,
			SqlStringGenerationContext context) {
		final StringBuilder definition = new StringBuilder();
		appendColumn( definition, column, table, metadata, dialect, context );
		return definition.toString();
	}


	static String getColumnDefinition(Column column, Metadata metadata, Dialect dialect) {
		final StringBuilder definition = new StringBuilder();
		appendColumnDefinition( definition, column, metadata, dialect );
		appendComment( definition, column, dialect );
		return definition.toString();
	}

	static void appendColumn(
			StringBuilder statement,
			Column column,
			Table table,
			Metadata metadata,
			Dialect dialect,
			SqlStringGenerationContext context) {
		statement.append( column.getQuotedName( dialect ) );
		appendColumnDefinition( statement, column, metadata, dialect );
		appendComment( statement, column, dialect );
		appendConstraints( statement, column, table, dialect, context );
		appendOptions(statement, column, dialect);
	}

	private static void appendOptions(StringBuilder statement, Column column, Dialect dialect) {
		final String options = column.getOptions();
		if ( isNotEmpty( options ) ) {
			statement.append( " " ).append( options );
		}
	}

	private static void appendConstraints(
			StringBuilder definition,
			Column column,
			Table table,
			Dialect dialect,
			SqlStringGenerationContext context) {
		if ( column.isUnique() && !table.isPrimaryKey( column ) ) {
			String uniqueKeyName = column.getUniqueKeyName();
			final String keyName = uniqueKeyName == null
					// fallback in case the ImplicitNamingStrategy name was not assigned
					// (we don't have access to the ImplicitNamingStrategy here)
					? generateName( "UK_", table, column )
					: uniqueKeyName;
			final UniqueKey uniqueKey = table.getOrCreateUniqueKey( keyName );
			uniqueKey.addColumn( column );
			definition.append( dialect.getUniqueDelegate().getColumnDefinitionUniquenessFragment( column, context ) );
		}

		if ( dialect.supportsColumnCheck() ) {
			// some databases (Maria, SQL Server) don't like multiple 'check' clauses
			final List<CheckConstraint> checkConstraints = column.getCheckConstraints();
			long anonConstraints = checkConstraints.stream().filter(CheckConstraint::isAnonymous).count();
			if ( anonConstraints == 1 ) {
				for ( CheckConstraint constraint : checkConstraints ) {
					definition.append( constraint.constraintString( dialect ) );
				}
			}
			else {
				boolean first = true;
				for ( CheckConstraint constraint : checkConstraints ) {
					if ( constraint.isAnonymous() ) {
						if ( first ) {
							definition.append(" check (");
							first = false;
						}
						else {
							definition.append(" and ");
						}
						definition.append( constraint.getConstraintInParens() );
					}
				}
				if ( !first ) {
					definition.append(")");
				}
				for ( CheckConstraint constraint : checkConstraints ) {
					if ( constraint.isNamed() ) {
						definition.append( constraint.constraintString( dialect ) );
					}
				}
			}
		}
	}

	private static void appendComment(StringBuilder definition, Column column, Dialect dialect) {
		final String columnComment = column.getComment();
		if ( columnComment != null ) {
			definition.append( dialect.getColumnComment( columnComment ) );
		}
	}

	private static void appendColumnDefinition(
			StringBuilder definition,
			Column column,
			Metadata metadata,
			Dialect dialect) {
		if ( column.isIdentity() ) {
			// to support dialects that have their own identity data type
			if ( dialect.getIdentityColumnSupport().hasDataTypeInIdentityColumn() ) {
				definition.append( ' ' ).append( column.getSqlType( metadata ) );
			}
			final String identityColumnString = dialect.getIdentityColumnSupport()
					.getIdentityColumnString( column.getSqlTypeCode( metadata ) );
			// the custom columnDefinition might have already included the
			// identity column generation clause, so try not to add it twice
			if ( !definition.toString().toLowerCase(Locale.ROOT).contains( identityColumnString ) ) {
				definition.append( ' ' ).append( identityColumnString );
			}
		}
		else {
			final String columnType = column.getSqlType( metadata );
			if ( column.getGeneratedAs() == null || dialect.hasDataTypeBeforeGeneratedAs() ) {
				definition.append( ' ' ).append( columnType );
			}

			final String collation = column.getCollation();
			if ( collation != null ) {
				definition.append(" collate ").append( dialect.quoteCollation( collation ) );
			}

			final String defaultValue = column.getDefaultValue();
			if ( defaultValue != null ) {
				definition.append( " default " ).append( defaultValue );
			}

			final String generatedAs = column.getGeneratedAs();
			if ( generatedAs != null) {
				definition.append( dialect.generatedAs( generatedAs ) );
			}

			if ( column.isNullable() ) {
				definition.append( dialect.getNullColumnString( columnType ) );
			}
			else {
				definition.append( " not null" );
			}
		}
	}

	private static String normalize(String typeName) {
		if ( typeName == null ) {
			return null;
		}
		else {
			final String lowercaseTypeName = typeName.toLowerCase(Locale.ROOT);
			return switch ( lowercaseTypeName ) {
				case "int" -> "integer";
				case "character" -> "char";
				case "character varying" -> "varchar";
				case "binary varying" -> "varbinary";
				case "character large object" -> "clob";
				case "binary large object" -> "blob";
				case "interval second" -> "interval";
				case "double precision" -> "double";
				// todo: normalize DECIMAL to NUMERIC?
				//       normalize REAL to FLOAT?
				default -> lowercaseTypeName;
			};
		}
	}

	private static String stripArgs(String typeExpression) {
		if ( typeExpression == null ) {
			return null;
		}
		else {
			final int i = typeExpression.indexOf('(');
			return i>0 ? typeExpression.substring(0,i).trim() : typeExpression;
		}
	}

	/**
	 * If a constraint is not explicitly named, this is called to generate
	 * a unique hash using the table and column names.
	 * Static so the name can be generated prior to creating the Constraint.
	 * They're cached, keyed by name, in multiple locations.
	 *
	 * @return String The generated name
	 *
	 * @deprecated This method does not respect the
	 *             {@link org.hibernate.boot.model.naming.ImplicitNamingStrategy}
	 */
	@Deprecated(since = "6.5", forRemoval = true)
	private static String generateName(String prefix, Table table, Column... columns) {
		// Use a concatenation that guarantees uniqueness, even if identical names
		// exist between all table and column identifiers.
		final StringBuilder sb = new StringBuilder( "table`" + table.getName() + "`" );
		// Ensure a consistent ordering of columns, regardless of the order
		// they were bound.
		// Clone the list, as sometimes a set of order-dependent Column
		// bindings are given.
		final Column[] alphabeticalColumns = columns.clone();
		Arrays.sort( alphabeticalColumns, Comparator.comparing( Column::getName ) );
		for ( Column column : alphabeticalColumns ) {
			final String columnName = column == null ? "" : column.getName();
			sb.append( "column`" ).append( columnName ).append( "`" );
		}
		return prefix + hashedName( sb.toString() );
	}

	/**
	 * Hash a constraint name using MD5. Convert the MD5 digest to base 35
	 * (full alphanumeric), guaranteeing
	 * that the length of the name will always be smaller than the 30
	 * character identifier restriction enforced by a few dialects.
	 *
	 * @param name The name to be hashed.
	 * @return String The hashed name.
	 *
	 * @deprecated Only used from deprecated methods
	 */
	@Deprecated(since = "6.5", forRemoval = true)
	private static String hashedName(String name) {
		try {
			final MessageDigest md = MessageDigest.getInstance( "MD5" );
			md.reset();
			md.update( name.getBytes() );
			final byte[] digest = md.digest();
			final BigInteger bigInt = new BigInteger( 1, digest );
			// By converting to base 35 (full alphanumeric), we guarantee
			// that the length of the name will always be smaller than the 30
			// character identifier restriction enforced by a few dialects.
			return bigInt.toString( 35 );
		}
		catch ( NoSuchAlgorithmException e ) {
			throw new HibernateException( "Unable to generate a hashed Constraint name", e );
		}
	}
}
