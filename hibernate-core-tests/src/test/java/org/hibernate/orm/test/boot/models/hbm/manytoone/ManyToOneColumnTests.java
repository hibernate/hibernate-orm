package org.hibernate.orm.test.boot.models.hbm.manytoone;

import org.hibernate.boot.Metadata;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.Column;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * The column of a {@code <many-to-one/>} keeps its {@code unique} and {@code sql-type}
 * settings, whether or not the {@code hbm.xml} mapping is transformed.
 */
@JiraKey("HHH-21209")
@SuppressWarnings("JUnitMalformedDeclaration")
public class ManyToOneColumnTests {
	private static final String MAPPING = "mappings/models/hbm/manytoone/columns.hbm.xml";

	@Test
	@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "false"))
	@DomainModel(xmlMappings = MAPPING)
	void testHbm(DomainModelScope scope) {
		verifyColumns( scope.getDomainModel() );
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "true"))
	@DomainModel(xmlMappings = MAPPING)
	void testTransformed(DomainModelScope scope) {
		verifyColumns( scope.getDomainModel() );
	}

	private static void verifyColumns(Metadata metadata) {
		assertSoftly( softly -> {
			softly.assertThat( targetColumn( metadata, UniqueSource.class ).isUnique() )
					.as( "unique attribute" )
					.isTrue();
			softly.assertThat( targetColumn( metadata, UniqueColumnSource.class ).isUnique() )
					.as( "unique column" )
					.isTrue();
			softly.assertThat( targetColumn( metadata, TypedColumnSource.class ).getSqlType() )
					.as( "sql-type" )
					.isEqualTo( "numeric(19,0)" );
			softly.assertThat( targetColumn( metadata, TypedColumnSource.class ).isUnique() )
					.as( "not unique" )
					.isFalse();
		} );
	}

	private static Column targetColumn(Metadata metadata, Class<?> entity) {
		return metadata.getEntityBinding( entity.getName() ).getProperty( "target" ).getColumns().get( 0 );
	}

	@SuppressWarnings("unused")
	public static class Target {
		private Long id;
	}

	@SuppressWarnings("unused")
	public static class UniqueSource {
		private Long id;
		private Target target;
	}

	@SuppressWarnings("unused")
	public static class UniqueColumnSource {
		private Long id;
		private Target target;
	}

	@SuppressWarnings("unused")
	public static class TypedColumnSource {
		private Long id;
		private Target target;
	}
}
