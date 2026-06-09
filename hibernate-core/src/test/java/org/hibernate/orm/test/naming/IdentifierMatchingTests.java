/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.naming;

import org.hibernate.boot.model.naming.Identifier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentifierMatchingTests {
	@Test
	void matchesUnquotedIdentifiersCaseInsensitively() {
		assertThat( Identifier.toIdentifier( "id" ).matches( Identifier.toIdentifier( "ID" ) ) ).isTrue();
	}

	@Test
	void matchesQuotedIdentifiersCaseSensitively() {
		assertThat( Identifier.toIdentifier( "`id`" ).matches( Identifier.toIdentifier( "`id`" ) ) ).isTrue();
		assertThat( Identifier.toIdentifier( "`id`" ).matches( Identifier.toIdentifier( "`ID`" ) ) ).isFalse();
	}

	@Test
	void doesNotMatchMixedQuotedAndUnquotedIdentifiers() {
		assertThat( Identifier.toIdentifier( "`id`" ).matches( Identifier.toIdentifier( "id" ) ) ).isFalse();
		assertThat( Identifier.toIdentifier( "id" ).matches( Identifier.toIdentifier( "`id`" ) ) ).isFalse();
	}
}
