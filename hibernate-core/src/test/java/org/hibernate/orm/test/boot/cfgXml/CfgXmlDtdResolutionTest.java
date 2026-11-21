package org.hibernate.orm.test.boot.cfgXml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.hibernate.HibernateException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Verifies local external-entity resolution for mapping XML and rejection by default.
///
/// @author Steve Ebersole
@BaseUnitTest
@JiraKey("HHH-21047")
public class CfgXmlDtdResolutionTest {
	private static final String XML = """
			<!DOCTYPE entity-mappings [
				<!ENTITY description SYSTEM "classpath://org/hibernate/orm/test/boot/cfgXml/mapping-description.xml">
			]>
			<entity-mappings xmlns="https://jakarta.ee/xml/ns/persistence/orm" version="3.2">
				<description>&description;</description>
			</entity-mappings>
			""";

	@Test
	void externalEntityResolvedFromClasspathWhenAllowed() {
		try ( var registry = new StandardServiceRegistryBuilder()
				.applySetting( MappingSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, true ).build() ) {
			final var binding = new MappingBinder( registry ).bind(
					new ByteArrayInputStream( XML.getBytes( StandardCharsets.UTF_8 ) ), new Origin( SourceType.INPUT_STREAM, "mapping" ) );
			assertThat( binding.getRoot().getDescription() ).isEqualTo( "resolved" );
		}
	}

	@Test
	void dtdRejectedByDefault() {
		try ( var registry = new StandardServiceRegistryBuilder().build() ) {
			assertThatThrownBy( () -> new MappingBinder( registry ).bind(
					new ByteArrayInputStream( XML.getBytes( StandardCharsets.UTF_8 ) ), new Origin( SourceType.INPUT_STREAM, "mapping" ) ) )
					.isInstanceOf( HibernateException.class );
		}
	}
}
