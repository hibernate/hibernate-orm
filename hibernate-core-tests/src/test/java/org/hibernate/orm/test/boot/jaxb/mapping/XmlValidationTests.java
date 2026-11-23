package org.hibernate.orm.test.boot.jaxb.mapping;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.hibernate.boot.InvalidMappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.MappingSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JiraKey("HHH-21129")
class XmlValidationTests {

	@ParameterizedTest
	@CsvSource({ ", true", "true, true", "false, false" })
	void hbmXml(String setting, boolean validate) {
		final String xml = """
				<?xml version="1.0"?>
				<!DOCTYPE hibernate-mapping PUBLIC
					"-//Hibernate/Hibernate Mapping DTD 3.0//EN"
					"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
				<hibernate-mapping>
					<class name="Price">
						<composite-id>
							<key-property name="code" length="20"/>
							<key-property name="amount">
								<column name="amount" precision="12" scale="3"/>
							</key-property>
						</composite-id>
					</class>
				</hibernate-mapping>
				""";
		assertValidation( setting, validate, xml,
				xml.replace( "<key-property name=\"amount\">",
						"<key-property name=\"amount\" precision=\"12\" scale=\"3\">" ),
				"Attribute 'precision' is not allowed to appear in element 'key-property'" );
	}

	@ParameterizedTest
	@CsvSource({ ", true", "true, true", "false, false" })
	void ormXml(String setting, boolean validate) {
		final String xml = """
				<entity-mappings xmlns="https://jakarta.ee/xml/ns/persistence/orm" version="4.0">
					<named-query name="Price.all">
						<query>from Price</query>
					</named-query>
					<sql-result-set-mapping name="Price.summary">
						<constructor-result target-class="PriceSummary">
							<column name="id"/>
						</constructor-result>
						<constructor-result target-class="PriceSummary">
							<entity entity-class="Price"/>
						</constructor-result>
					</sql-result-set-mapping>
					<entity class="Price">
						<attributes>
							<id name="id"/>
						</attributes>
					</entity>
				</entity-mappings>
				""";
		assertValidation( setting, validate, xml,
				xml.replace( "<entity class=\"Price\">", "<entity class=\"Price\" invalid=\"ignored\">" ),
				"Attribute 'invalid' is not allowed to appear in element 'entity'" );
	}

	private void assertValidation(String setting, boolean validate, String validXml, String invalidXml,
			String errorMessage) {
		final var builder = new StandardServiceRegistryBuilder();
		builder.getSettings().remove( MappingSettings.VALIDATE_XML );
		if ( setting != null ) {
			builder.applySetting( MappingSettings.VALIDATE_XML, setting );
		}
		try ( var registry = builder.build() ) {
			final var sources = new MetadataSources( registry );
			assertDoesNotThrow( () -> sources.addInputStream( stream( validXml ) ) );
			if ( validate ) {
				final var exception = assertThrows( InvalidMappingException.class,
						() -> sources.addInputStream( stream( invalidXml ) ) );
				assertThat( exception ).hasStackTraceContaining( errorMessage );
			}
			else {
				assertDoesNotThrow( () -> sources.addInputStream( stream( invalidXml ) ) );
			}
		}
	}

	private static ByteArrayInputStream stream(String xml) {
		return new ByteArrayInputStream( xml.getBytes( StandardCharsets.UTF_8 ) );
	}
}
