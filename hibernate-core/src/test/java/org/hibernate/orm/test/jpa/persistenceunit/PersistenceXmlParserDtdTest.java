/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.persistenceunit;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.MappingSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES;

@JiraKey("HHH-21047")
public class PersistenceXmlParserDtdTest {
	private static final String DTD_ENTITY_PU = "dtd-entity";

	@Test
	public void parse_rejectsDtdAndExternalEntitiesByDefault() {
		assertThatThrownBy( () -> parse( Collections.emptyMap() ) )
				.isInstanceOf( PersistenceException.class );
	}

	@Test
	public void parse_integrationSettingFalseRejectsDtdAndExternalEntities() {
		assertThatThrownBy( () -> parse( Map.of( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, false ) ) )
				.isInstanceOf( PersistenceException.class );
	}

	private Map<String, ParsedPersistenceXmlDescriptor> parse(Map<String, Object> integration) {
		final URL url = getClass().getClassLoader()
				.getResource( "org/hibernate/jpa/test/persistenceunit/META-INF/" + DTD_ENTITY_PU + ".xml" );
		assertThat( url ).isNotNull();
		return PersistenceXmlParser.parse( url, PersistenceUnitTransactionType.RESOURCE_LOCAL, integration );
	}
}
