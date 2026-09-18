/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.naming;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/// Guards conversion flags and rendering when normalizing without a building context.
///
/// @author Steve Ebersole
class ObjectNameNormalizerTest {
	@Test
	void preservesImplicitConversionAndDialectRendering() {
		final var helper = mock( IdentifierHelper.class );
		final var normalizer = new ObjectNameNormalizer( helper, new H2Dialect() );
		assertThat( normalizer.normalizeIdentifierQuoting( (String) null ) ).isNull();
		assertThat( normalizer.normalizeIdentifierQuotingAsString( null ) ).isNull();
		assertThat( normalizer.toDatabaseIdentifierText( null ) ).isNull();
		verifyNoInteractions( helper );

		when( helper.toIdentifier( "name", false, false ) ).thenReturn( Identifier.toIdentifier( "name", true ) );
		assertThat( normalizer.normalizeIdentifierQuotingAsString( "name" ) ).isEqualTo( "\"name\"" );
		assertThat( normalizer.toDatabaseIdentifierText( "name" ) ).isEqualTo( "\"name\"" );

		when( helper.applyGlobalQuoting( "varchar(255)" ) )
				.thenReturn( Identifier.toIdentifier( "varchar(255)", true ) );
		assertThat( normalizer.applyGlobalQuoting( "varchar(255)" ) ).isEqualTo( "\"varchar(255)\"" );
	}
}
