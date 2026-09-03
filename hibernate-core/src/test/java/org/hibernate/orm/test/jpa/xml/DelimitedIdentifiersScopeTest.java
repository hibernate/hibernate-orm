/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.xml;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Jira("https://hibernate.atlassian.net/browse/HHH-13347")
@ServiceRegistry
public class DelimitedIdentifiersScopeTest {
	@Test
	void xmlDefaultIsScopedToThePersistenceUnit(ServiceRegistryScope scope) {
		final var serviceRegistry = scope.getRegistry();
		final var quotedMetadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( DelimitedIdentifiersTest.SequenceGeneratedEntity.class )
				.addResource( "org/hibernate/orm/test/jpa/xml/delimited-identifiers.xml" )
				.buildMetadata();
		final var unquotedMetadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( DelimitedIdentifiersTest.SequenceGeneratedEntity.class )
				.buildMetadata();

		assertThat( quotedMetadata.getEntityBinding( DelimitedIdentifiersTest.SequenceGeneratedEntity.class.getName() )
				.getTable().isQuoted() )
				.isTrue();
		assertThat( quotedMetadata.getDatabase().toIdentifier( "QuotedIdentifier" ).isQuoted() ).isTrue();

		assertThat( unquotedMetadata.getEntityBinding( DelimitedIdentifiersTest.SequenceGeneratedEntity.class.getName() )
				.getTable().isQuoted() )
				.isFalse();
		assertThat( unquotedMetadata.getDatabase().toIdentifier( "UnquotedIdentifier" ).isQuoted() ).isFalse();
		assertThat( serviceRegistry.requireService( JdbcEnvironment.class )
				.getIdentifierHelper().toIdentifier( "RegistryIdentifier" ).isQuoted() )
				.isFalse();
	}
}
