package org.hibernate.boot.model.naming;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingContext;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * Provides centralized normalization of how database object names are handled.
 *
 * @author Steve Ebersole
 */
public class ObjectNameNormalizer {
	private final MetadataBuildingContext context;

	public ObjectNameNormalizer(@Nonnull MetadataBuildingContext context) {
		this.context = context;
	}

	/**
	 * Normalizes the quoting of identifiers.
	 * <p>
	 * This implements the rules set forth in JPA 2 (section "2.13 Naming of Database Objects") which
	 * states that the double-quote (") is the character which should be used to denote a {@code quoted
	 * identifier}.  Here, we handle recognizing that and converting it to the more elegant
	 * backtick (`) approach used in Hibernate.  Additionally, we account for applying what JPA2 terms
	 * "globally quoted identifiers".
	 *
	 * @param identifierText The identifier to be quoting-normalized.
	 * @return The identifier accounting for any quoting that need be applied.
	 */
	public Identifier normalizeIdentifierQuoting(String identifierText) {
		return database().toIdentifier( identifierText );
	}

	protected Database database() {
		return getBuildingContext().getMetadataCollector().getDatabase();
	}

	public Identifier normalizeIdentifierQuoting(Identifier identifier) {
		return database().getJdbcEnvironment().getIdentifierHelper()
				.normalizeQuoting( identifier );
	}

	/**
	 * Normalizes the quoting of identifiers.  This form returns a String rather than an Identifier
	 * to better work with the legacy code in {@link org.hibernate.mapping}
	 *
	 * @param identifierText The identifier to be quoting-normalized.
	 * @return The identifier accounting for any quoting that need be applied.
	 */
	public String normalizeIdentifierQuotingAsString(String identifierText) {
		final Identifier identifier = normalizeIdentifierQuoting( identifierText );
		return identifier == null ? null : identifier.render( database().getDialect() );
	}

	public String toDatabaseIdentifierText(String identifierText) {
		return database().getDialect().quote( normalizeIdentifierQuotingAsString( identifierText ) );
	}

	/**
	 * Determine the logical name give a (potentially {@code null}/empty) explicit name.
	 *
	 * @param explicitName The explicit, user-supplied name
	 * @param namingStrategyHelper The naming strategy helper.
	 *
	 * @return The logical name
	 */
	public Identifier determineLogicalName(String explicitName, NamingStrategyHelper namingStrategyHelper) {
		final Identifier logicalName =
				isEmpty( explicitName )
						? namingStrategyHelper.determineImplicitName( getBuildingContext() )
						: namingStrategyHelper.handleExplicitName( explicitName, getBuildingContext() );
		return database().getJdbcEnvironment().getIdentifierHelper().normalizeQuoting( logicalName );
	}

	/**
	 * Intended only for use in handling quoting requirements for {@code column-definition}
	 * as defined by {@link jakarta.persistence.Column#columnDefinition()},
	 *  {@link jakarta.persistence.JoinColumn#columnDefinition}, etc.  This method should not
	 * be called in any other scenario.
	 *
	 * @param text The specified column definition
	 *
	 * @return The name with global quoting applied, or {@code null} for null or blank text
	 */
	@Nullable
	public String applyGlobalQuoting(@Nullable String text) {
		final var identifier =
				database().getJdbcEnvironment().getIdentifierHelper()
						.applyGlobalQuoting( text );
		return identifier == null
				? null
				: identifier.render( database().getDialect() );
	}


	/**
	 * Access the contextual information related to the current process of building metadata.  Here,
	 * that typically might be needed for accessing:<ul>
	 *     <li>{@link ImplicitNamingStrategy}</li>
	 *     <li>{@link PhysicalNamingStrategy}</li>
	 *     <li>{@link Database}</li>
	 * </ul>
	 *
	 * @return The current building context
	 */
	protected MetadataBuildingContext getBuildingContext() {
		return context;
	}
}
