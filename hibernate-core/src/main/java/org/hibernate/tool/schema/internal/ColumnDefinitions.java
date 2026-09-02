/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.NamingHelper;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.constraint.spi.CheckConstraintPlacement;
import org.hibernate.dialect.constraint.spi.CheckConstraintRenderRequest;
import org.hibernate.dialect.schema.spi.CommentPlacement;
import org.hibernate.dialect.schema.spi.CommentRequest;
import org.hibernate.dialect.schema.spi.CommentTarget;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Locale;

import static java.util.Comparator.comparing;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.type.SqlTypes.isNumericOrDecimal;
import static org.hibernate.type.SqlTypes.isStringType;

public class ColumnDefinitions {

	public static boolean hasMatchingType(
			Column column,
			ColumnInformation columnInformation,
			Metadata metadata,
			Dialect dialect) {
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

	public static boolean hasMatchingLength(
			Column column,
			ColumnInformation columnInformation,
			Metadata metadata,
			Dialect dialect) {
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

	public static String getFullColumnDeclaration(
			Column column,
			Table table,
			Metadata metadata,
			Dialect dialect,
			SqlStringGenerationContext context) {
		final StringBuilder definition = new StringBuilder();
		appendColumn( definition, column, table, metadata, dialect, context );
		return definition.toString();
	}


	public static String getColumnDefinition(Column column, Metadata metadata, Dialect dialect) {
		final StringBuilder definition = new StringBuilder();
		appendColumnDefinition( definition, column, metadata, dialect );
		appendComment( definition, column, dialect );
		return definition.toString();
	}

	public static void appendColumn(
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
			final String uniqueKeyName = column.getUniqueKeyName();
			final String keyName = uniqueKeyName == null
					// fallback in case the ImplicitNamingStrategy name was not assigned
					// (we don't have access to the ImplicitNamingStrategy here)
					? generateName( "UK_", table, column )
					: uniqueKeyName;
			final var uniqueKey = table.getOrCreateUniqueKey( keyName );
			uniqueKey.addColumn( column );
			definition.append( dialect.getUniqueDelegate().getColumnDefinitionUniquenessFragment( column, context ) );
		}

		final var checkSupport = dialect.getCheckConstraintSupport();
		if ( checkSupport.supports( CheckConstraintPlacement.ANONYMOUS_COLUMN ) ) {
			final var checkConstraints = column.getCheckConstraints();
			boolean hasAnonymousConstraints = false;
			for ( var constraint : checkConstraints ) {
				if ( constraint.isAnonymous() ) {
					if ( !hasAnonymousConstraints ) {
						definition.append(" check (");
						hasAnonymousConstraints = true;
					}
					else {
						definition.append(" and ");
					}
					definition.append( constraint.getConstraintInParens() );
				}
			}
			if ( hasAnonymousConstraints ) {
				definition.append( ')' );
			}

			if ( !checkSupport.supports( CheckConstraintPlacement.TABLE ) ) {
				// When table check constraints are not supported, try to render all named constraints
				for ( var constraint : checkConstraints ) {
					if ( constraint.isNamed() ) {
						definition.append( ' ' ).append( checkSupport.render( new CheckConstraintRenderRequest(
								CheckConstraintPlacement.NAMED_COLUMN,
								constraint.getName(),
								constraint.getConstraint(),
								constraint.getOptions()
						) ) );
					}
				}
			}
			else if ( !hasAnonymousConstraints && checkSupport.supports( CheckConstraintPlacement.NAMED_COLUMN ) ) {
				// Otherwise only render the first named constraint as column constraint if there are no anonymous
				// constraints and named column check constraint are supported, because some database don't like
				// multiple check clauses.
				// Note that the TableExporter will take care of named constraints then
				for ( var constraint : checkConstraints ) {
					if ( constraint.isNamed() ) {
						definition.append( ' ' ).append( checkSupport.render( new CheckConstraintRenderRequest(
								CheckConstraintPlacement.NAMED_COLUMN,
								constraint.getName(),
								constraint.getConstraint(),
								constraint.getOptions()
						) ) );
						break;
					}
				}
			}
		}
	}

	private static void appendComment(StringBuilder definition, Column column, Dialect dialect) {
		final String columnComment = column.getComment();
		if ( columnComment != null ) {
			final var support = dialect.getSchemaCommentSupport();
			if ( support.placement( CommentTarget.TABLE_COLUMN ) == CommentPlacement.INLINE ) {
				definition.append( support.render( new CommentRequest(
						CommentTarget.TABLE_COLUMN,
						column.getQuotedName( dialect ),
						columnComment
				) ) );
			}
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
			final String identityColumnString =
					dialect.getIdentityColumnSupport()
							.getIdentityColumnString( column.getSqlTypeCode( metadata ) );
			// the custom columnDefinition might have already included the
			// identity column generation clause, so try not to add it twice
			if ( !definition.toString().toLowerCase(Locale.ROOT).contains( identityColumnString ) ) {
				definition.append( ' ' ).append( identityColumnString );
			}
		}
		else {
			final String columnType = column.getSqlType( metadata );
			final String collation = column.getCollation();
			dialect.getColumnDefinitionSupport().appendDefinition(
					new StringBuilderSqlAppender( definition ),
					new ColumnDefinitionRequest(
							columnType,
							collation == null ? null : dialect.getIdentifierSupport().quoteCollation( collation ),
							column.isNullable(),
							column.getDefaultValue(),
							column.getGeneratedAs()
					)
			);
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
		final var builder = new StringBuilder( "table`" + table.getName() + "`" );
		// Ensure a consistent ordering of columns, regardless of the order
		// they were bound.
		// Clone the list, as sometimes a set of order-dependent Column
		// bindings are given.
		final var alphabeticalColumns = columns.clone();
		Arrays.sort( alphabeticalColumns, comparing( Column::getName ) );
		for ( var column : alphabeticalColumns ) {
			final String columnName = column == null ? "" : column.getName();
			builder.append( "column`" ).append( columnName ).append( "`" );
		}
		final byte[] hashed = NamingHelper.hash( builder.toString().getBytes() );
		return prefix + new BigInteger( 1, hashed ).toString( 35 );
	}

}
