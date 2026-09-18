/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;

/**
 * Provides centralized normalization of how database object names are handled.
 *
 * @author Steve Ebersole
 */
public class ObjectNameNormalizer {
	private final IdentifierHelper identifierHelper;
	private final Dialect dialect;

	public ObjectNameNormalizer(IdentifierHelper identifierHelper, Dialect dialect) {
		this.identifierHelper = identifierHelper;
		this.dialect = dialect;
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
		return identifierText == null ? null : identifierHelper.toIdentifier( identifierText, false, false );
	}

	public Identifier normalizeIdentifierQuoting(Identifier identifier) {
		return identifierHelper.normalizeQuoting( identifier );
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
		return identifier == null ? null : identifier.render( dialect );
	}

	public String toDatabaseIdentifierText(String identifierText) {
		return dialect.quote( normalizeIdentifierQuotingAsString( identifierText ) );
	}

	/**
	 * Intended only for use in handling quoting requirements for {@code column-definition}
	 * as defined by {@link jakarta.persistence.Column#columnDefinition()},
	 *  {@link jakarta.persistence.JoinColumn#columnDefinition}, etc.  This method should not
	 * be called in any other scenario.
	 *
	 * @param text The specified column definition
	 *
	 * @return The name with global quoting applied
	 */
	public String applyGlobalQuoting(String text) {
		return identifierHelper.applyGlobalQuoting( text ).render( dialect );
	}

}
