package org.hibernate.orm.test.boot.models.xml;

import org.hibernate.mapping.PhysicalTable;
import org.hibernate.MappingException;

import org.hibernate.boot.pipeline.internal.MetadataBuildingHelper;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.pipeline.internal.source.XmlMappingSource;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.mapping.ManyToOne;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Explicit property references must recognize uniqueness represented by a dialect as an index.
/// No database connection is needed to reproduce the metadata validation failure.
///
/// @author Steve Ebersole
public class PropertyRefUniqueIndexTest {
	static Stream<Arguments> mappings() {
		return Stream.of( H2Dialect.class, SpannerDialect.class ).flatMap( dialect ->
				Stream.of( "7.0", "8.0" ).flatMap( version ->
						Stream.of( 1, 2 ).map( span -> Arguments.of( dialect, version, span ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void uniqueConstraintSupportsPropertyRef(Class<?> dialect, String version, int span) {
		var metadata = withMapping( dialect, version, span, true, false );
		var association = (ManyToOne) metadata.getEntityBinding( Owner.class.getName() )
				.getProperty( "target" ).getValue();
		assertThat( association.getReferencedPropertyName() ).isEqualTo( "lookup" );
		assertThat( association.isReferenceToPrimaryKey() ).isFalse();
		assertThat( association.getColumnSpan() ).isEqualTo( span );
		if ( dialect == SpannerDialect.class ) {
			var table = (PhysicalTable) metadata.getEntityBinding( Target.class.getName() ).getTable();
			assertThat( table.getIndexes().values() ).anySatisfy( index -> {
				assertThat( index.isUnique() ).isTrue();
				assertThat( index.getColumnSpan() ).isEqualTo( span );
			} );
		}
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void nonUniqueIndexDoesNotEstablishUniqueness(Class<?> dialect, String version, int span) {
		assertThrows( MappingException.class, () -> withMapping( dialect, version, span, false, false ) );
	}

	@ParameterizedTest
	@MethodSource("mappings")
	void widerUniqueKeyDoesNotEstablishUniqueness(Class<?> dialect, String version, int span) {
		assertThrows( MappingException.class, () -> withMapping( dialect, version, span, true, true ) );
	}

	@Test
	void implicitJoinColumnsUseReferencedPropertyShape() {
		final var metadata = withMapping( H2Dialect.class, "9.0", 2, true, false, true );
		final var association = (ManyToOne) metadata.getEntityBinding( Owner.class.getName() ).getProperty( "target" ).getValue();
		assertThat( association.getReferencedPropertyName() ).isEqualTo( "lookup" );
		assertThat( association.getColumnSpan() ).isEqualTo( 2 );
	}

	private static Metadata withMapping(Class<?> dialect, String version, int span, boolean unique, boolean wider) {
		return withMapping( dialect, version, span, unique, wider, false );
	}

	private static Metadata withMapping(Class<?> dialect, String version, int span, boolean unique, boolean wider, boolean implicit) {
		String secondColumn = span == 2 ? "<column-name>lookup_kind</column-name>" : "";
		String declaration = unique
				? "<unique-constraint><column-name>lookup_code</column-name>" + secondColumn
						+ (wider ? "<column-name>extra</column-name>" : "") + "</unique-constraint>"
				: "<index name=\"non_unique_lookup\" column-list=\"lookup_code"
						+ (span == 2 ? ",lookup_kind" : "") + "\"/>";
		String xml = """
				<entity-mappings xmlns="https://www.hibernate.org/xsd/orm/mapping" version="%s">
					<entity class="%s" name="IndexRefOwner" access="FIELD" metadata-complete="true">
						<table name="index_ref_owner"/>
						<attributes>
							<id name="id"/>
							<one-to-one name="target" target-entity="%s">
								<join-column name="target_code"/>%s
								<property-ref name="lookup"/>
							</one-to-one>
						</attributes>
					</entity>
					<entity class="%s" name="IndexRefTarget" access="FIELD" metadata-complete="true">
						<table name="index_ref_target">%s</table>
						<attributes><id name="id"/><basic name="extra"/><embedded name="lookup"/></attributes>
					</entity>
					<embeddable class="%s" access="FIELD" metadata-complete="true">
						<attributes>
							<basic name="code"><column name="lookup_code"/></basic>
							%s
						</attributes>
					</embeddable>
				</entity-mappings>
				""".formatted( version, Owner.class.getName(), Target.class.getName(),
				span == 2 ? "<join-column name=\"target_kind\"/>" : "", Target.class.getName(), declaration,
				Lookup.class.getName(), span == 2
						? "<basic name=\"kind\"><column name=\"lookup_kind\"/></basic>"
						: "<transient name=\"kind\"/>" );
		if ( implicit ) {
			xml = xml.replace( "<join-column name=\"target_code\"/>", "" )
					.replace( "<join-column name=\"target_kind\"/>", "" );
		}
		var registry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.dialect", dialect.getName() )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false )
				.applySetting( "hibernate.validate_xml", true )
				.build();
		try {
			var metadata = MetadataBuildingHelper.buildMetadata( registry, new MappingSources().addXmlMappingSource( XmlMappingSource.fromInputStream( new ByteArrayInputStream( xml.getBytes( StandardCharsets.UTF_8 ) ) ) ) );
			return metadata;
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	public static class Owner {
		Long id;
		Target target;
	}

	public static class Target {
		Long id;
		Long extra;
		Lookup lookup;
	}

	public static class Lookup implements Serializable {
		Long code;
		String kind;
	}
}
