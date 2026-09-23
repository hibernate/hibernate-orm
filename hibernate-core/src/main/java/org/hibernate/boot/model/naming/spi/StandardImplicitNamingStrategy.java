/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitConstraintNameSource;
import org.hibernate.boot.model.naming.ImplicitIndexNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.NamingHelper;
import org.hibernate.relational.naming.spi.LogicalName;

import java.io.Serializable;

import org.hibernate.SPI;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.unqualify;

/// Standard implicit naming algorithms, with role-specific callback overrides.
/// Table composition uses logical dependency names. Column and constraint callbacks
/// are being migrated separately to the new input contract.
///
/// @author Steve Ebersole
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
public class StandardImplicitNamingStrategy implements ImplicitNamingStrategy, Serializable {
	public static final ImplicitNamingStrategy INSTANCE = new StandardImplicitNamingStrategy();

	@SPI(SPI.Role.USE)
	public StandardImplicitNamingStrategy() {
	}

	@Override
	@Nonnull
	public LogicalName determinePrimaryTableName(@Nonnull PrimaryTableNamingInput input, @Nonnull ImplicitNamingContext context) {
		return context.implicitName( transformEntityName( input.entity() ) );
	}

	protected String transformEntityName(EntityNaming entityNaming) {
		return isNotEmpty( entityNaming.getJpaEntityName() )
				// prefer the JPA entity name, if specified
				? entityNaming.getJpaEntityName()
				// otherwise, use the unqualified Hibernate entity name
				: unqualify( entityNaming.getEntityName() );
	}


	@Override
	@Nonnull
	public LogicalName determineAssociationTableName(@Nonnull AssociationTableNamingInput input, @Nonnull ImplicitNamingContext context) {
		final var owner = input.owningTable().logicalName();
		final var target = input.targetTable().logicalName();
		return context.implicitName( owner.getText() + '_' + target.getText(), owner.isQuoted() || target.isQuoted() );
	}

	@Override
	@Nonnull
	public LogicalName determineCollectionTableName(@Nonnull CollectionTableNamingInput input, @Nonnull ImplicitNamingContext context) {
		return context.implicitName( transformEntityName( input.owner() ) + '_'
				+ transformAttributePath( AttributePath.parse( input.attributePath() ) ) );
	}


	@Override
	@Nonnull
	public LogicalName determineIdentifierColumnName(@Nonnull IdentifierColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		return context.implicitName( transformAttributePath( AttributePath.parse( input.attributePath() ) ) );
	}

	@Override
	@Nonnull
	public LogicalName determineAggregateColumnName(@Nonnull AggregateColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		return context.implicitName( input.attributeName() );
	}

	@Override
	@Nonnull
	public LogicalName determineTenantColumnName(@Nonnull TenantColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		return context.implicitName( AttributePath.parse( input.attributePath() ).getProperty() );
	}

	@Override
	@Nonnull
	public LogicalName determineBasicColumnName(@Nonnull BasicColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		return context.implicitName( transformAttributePath( AttributePath.parse( input.attributePath() ) ) );
	}

	@Override
	@Nonnull
	public LogicalName determineCollectionElementColumnName(@Nonnull CollectionElementColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		return context.implicitName( transformAttributePath( AttributePath.parse( input.attributePath() ) ) );
	}

	@Override
	@Nonnull
	public LogicalName determineJoinColumnName(@Nonnull JoinColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		return joinName( transformAttributePath( AttributePath.parse( input.attributePath() ) ), input.reference(), context );
	}

	@Override
	@Nonnull
	public LogicalName determineCollectionKeyColumnName(@Nonnull CollectionKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
		if ( input.kind() == CollectionKeyNamingInput.Kind.TO_ONE_TABLE ) {
			return referencedColumnName( input.reference(), context );
		}
		final String prefix = input.kind() == CollectionKeyNamingInput.Kind.ONE_TO_MANY
				? transformAttributePath( AttributePath.parse( input.attributePath() ) )
				: input.inverseAttributePath().map( path -> transformAttributePath( AttributePath.parse( path ) ) )
						.orElseGet( () -> transformEntityName( input.owner() ) );
		return joinName( prefix, input.reference(), context );
	}

	@Override
	@Nonnull
	public LogicalName determineAssociationKeyColumnName(@Nonnull AssociationKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
		return joinName( transformAttributePath( AttributePath.parse( input.attributePath() ) ), input.reference(), context );
	}

	@Override
	@Nonnull
	public LogicalName determineMapKeyJoinColumnName(@Nonnull MapKeyJoinColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		final String prefix = transformAttributePath( AttributePath.parse( input.attributePath() ) ) + "_KEY";
		return input.referencesPrimaryKey() && input.reference().columns().size() == 1
				? context.implicitName( prefix ) : joinName( prefix, input.reference(), context );
	}

	@Override
	@Nonnull
	public LogicalName determinePrimaryKeyJoinColumnName(@Nonnull PrimaryKeyJoinColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		return referencedColumnName( input.reference(), context );
	}

	protected LogicalName joinName(String prefix, ReferencedColumnsNamingInput reference, ImplicitNamingContext context) {
		final var column = reference.column().logicalName();
		return context.implicitName( prefix + '_' + column.getText(), column.isQuoted() );
	}

	protected LogicalName referencedColumnName(ReferencedColumnsNamingInput reference, ImplicitNamingContext context) {
		final var column = reference.column().logicalName();
		return context.implicitName( column.getText(), column.isQuoted() );
	}

	@Override
	@Nonnull
	public LogicalName determineAnyDiscriminatorColumnName(@Nonnull AnyColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		return context.implicitName( transformAttributePath( AttributePath.parse( input.attributePath() ) ) + "_" + context.getNamingDefaults().getDefaultDiscriminatorColumnName() );
	}

	@Override
	@Nonnull
	public LogicalName determineAnyKeyColumnName(@Nonnull AnyColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		return context.implicitName( transformAttributePath( AttributePath.parse( input.attributePath() ) ) + "_" + context.getNamingDefaults().getDefaultIdColumnName()
				+ (input.columnPosition() == 0 ? "" : Integer.toString( input.columnPosition() + 1 )) );
	}

	@Override
	@Nonnull
	public LogicalName determineMapKeyColumnName(@Nonnull MapKeyColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		return context.implicitName( transformAttributePath( AttributePath.parse( input.attributePath() ) ) + "_KEY" );
	}

	@Override
	@Nonnull
	public LogicalName determineListIndexColumnName(@Nonnull ListIndexColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
		return context.implicitName( transformAttributePath( AttributePath.parse( input.attributePath() ) ) + "_ORDER" );
	}


	@Override
	@Nonnull
	public Identifier determineIndexName(@Nonnull ImplicitIndexNameSource source) {
		final Identifier userProvidedIdentifier = source.getUserProvidedIdentifier();
		return userProvidedIdentifier == null
				? generateConstraintName( source )
				: userProvidedIdentifier;
	}

	/// Use the full attribute path so repeated embeddables have distinct default names.
	///
	/// @param attributePath The attribute path
	///
	/// @return The extracted name
	protected String transformAttributePath(AttributePath attributePath) {
		final StringBuilder result = new StringBuilder();
		appendAttributePath( attributePath, result );
		return result.toString();
	}

	/// Append the full attribute path, normalizing synthetic component segments.
	protected static void appendAttributePath(AttributePath path, StringBuilder result) {
		final var parent = path.getParent();
		final String property = path.getProperty();
		if ( parent != null && isNotEmpty( parent.getProperty() ) ) {
			appendAttributePath( parent, result );
			result.append( '_' );
		}
		else if ( org.hibernate.spi.NavigablePath.IDENTIFIER_MAPPER_PROPERTY.equals( property ) ) {
			result.append( "id" );
			return;
		}
		result.append( property.replace( "<", "" ).replace( ">", "" ) );
	}

		/// Easy hook to build an {@link Identifier} using the keyword safe
	/// {@link org.hibernate.engine.jdbc.env.spi.IdentifierHelper}.
	///
	/// @param stringForm The String form of the name
	/// @param namingContext Access to the {@code IdentifierHelper}
	///
	/// @return The identifier
	protected Identifier toIdentifier(String stringForm, ImplicitNamingContext namingContext) {
		return toIdentifier( stringForm,
				namingContext.getIdentifierHelper() );
	}

		/// Easy hook to build an {@link Identifier} using the keyword safe
	/// {@link org.hibernate.engine.jdbc.env.spi.IdentifierHelper}.
	///
	/// @param stringForm The String form of the name
	/// @param identifierHelper The {@code IdentifierHelper}
	///
	/// @return The identifier
	protected Identifier toIdentifier(String stringForm, IdentifierHelper identifierHelper) {
		return identifierHelper.toIdentifier( stringForm );
	}

		/// Generate a name for the given constraint.
	///
	/// @return The identifier
	protected Identifier generateConstraintName(ImplicitConstraintNameSource source) {
		return toIdentifier( generateConstraintNameString( source ), source.getNamingContext() );
	}

		/// Generate a name for the given constraint.
	///
	/// @return The name as a string
	protected String generateConstraintNameString(ImplicitConstraintNameSource source) {
		final var namingHelper = namingHelper( source.getNamingContext() );
		final String prefix = constraintNamePrefix( source.kind() );
		return namingHelper.generateHashedConstraintName(
				prefix, source.getTableName(), source.getColumnNames() );
	}

		/// Obtain a {@link NamingHelper} for use in constraint name generation.
	protected NamingHelper namingHelper(ImplicitNamingContext context) {
		return NamingHelper.withCharset( context.getSchemaCharset() );
	}

		/// The prefix for a generated constraint name of the given
	/// {@linkplain ImplicitConstraintNameSource.Kind kind}.
	///
	/// @return The prefix as a string
	protected String constraintNamePrefix(ImplicitConstraintNameSource.Kind kind) {
		return switch ( kind ) {
			case INDEX -> "IDX";
		};
	}
}
