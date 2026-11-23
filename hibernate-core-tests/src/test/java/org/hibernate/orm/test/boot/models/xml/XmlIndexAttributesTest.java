package org.hibernate.orm.test.boot.models.xml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.mapping.Index;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that the `type`, `using` and `options` attributes of an orm.xml `<index/>`
/// reach the boot-time [Index] model, the same way they do for `@jakarta.persistence.Index`.
@JiraKey("HHH-21065")
public class XmlIndexAttributesTest {
	private static final String INDEX_NAME = "idx_xml_index_entity_name";

	@Test
	void optionsAreAppliedForMapping70() {
		final Index index = bindIndex( "7.0", "options=\"some_options\"" );

		assertThat( index.getOptions() ).isEqualTo( "some_options" );
	}

	@Test
	void typeUsingAndOptionsAreAppliedForMapping80() {
		final Index index = bindIndex(
				"8.0",
				"type=\"some_type\" using=\"some_method\" options=\"some_options\""
		);

		assertThat( index.getType() ).isEqualTo( "some_type" );
		assertThat( index.getUsing() ).isEqualTo( "some_method" );
		assertThat( index.getOptions() ).isEqualTo( "some_options" );
	}

	private static Index bindIndex(String xsdVersion, String indexAttributes) {
		final String xml = """
				<entity-mappings xmlns="https://www.hibernate.org/xsd/orm/mapping" version="%s">
					<entity class="%s" name="XmlIndexEntity" access="FIELD" metadata-complete="true">
						<table name="xml_index_entity">
							<index name="%s" column-list="name" %s/>
						</table>
						<attributes>
							<id name="id"/>
							<basic name="name"/>
						</attributes>
					</entity>
				</entity-mappings>
				""".formatted( xsdVersion, IndexedEntity.class.getName(), INDEX_NAME, indexAttributes );
		final var registry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.dialect", H2Dialect.class.getName() )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false )
				.applySetting( "hibernate.validate_xml", true )
				.build();
		try {
			final var metadata = new MetadataSources( registry )
					.addInputStream( new ByteArrayInputStream( xml.getBytes( StandardCharsets.UTF_8 ) ) )
					.buildMetadata();
			final Index index = metadata.getEntityBinding( IndexedEntity.class.getName() )
					.getTable()
					.getIndex( INDEX_NAME );
			assertThat( index ).as( "Index declared in orm.xml" ).isNotNull();
			return index;
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	public static class IndexedEntity {
		Long id;
		String name;
	}
}
