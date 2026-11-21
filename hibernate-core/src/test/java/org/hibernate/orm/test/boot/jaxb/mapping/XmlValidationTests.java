package org.hibernate.orm.test.boot.jaxb.mapping;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.hibernate.boot.InvalidMappingException;
import org.hibernate.MappingException;
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
	void hbmXmlIsRejectedRegardlessOfValidation(String setting, boolean validate) {
		try ( var registry = new StandardServiceRegistryBuilder().build() ) {
			final var binder = new MappingBinder( registry );
			assertThrows( MappingException.class, () -> binder.bind(
					stream( "<hibernate-mapping/>" ), new Origin( SourceType.INPUT_STREAM, "legacy" ) ) );
		}
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
			final var binder = new MappingBinder( registry );
			assertDoesNotThrow( () -> binder.bind( stream( validXml ), new Origin( SourceType.INPUT_STREAM, "valid" ) ) );
			if ( validate ) {
				final var exception = assertThrows( InvalidMappingException.class,
						() -> binder.bind( stream( invalidXml ), new Origin( SourceType.INPUT_STREAM, "invalid" ) ) );
				assertThat( exception ).hasStackTraceContaining( errorMessage );
			}
			else {
				assertDoesNotThrow( () -> binder.bind( stream( invalidXml ), new Origin( SourceType.INPUT_STREAM, "invalid" ) ) );
			}
		}
	}

	private static ByteArrayInputStream stream(String xml) {
		return new ByteArrayInputStream( xml.getBytes( StandardCharsets.UTF_8 ) );
	}
}
