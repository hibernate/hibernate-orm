/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.intg;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry.JavaService;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.junit.jupiter.api.Test;

public class AdditionalMappingContributorBasicColumnTests {

	@Test
	@BootstrapServiceRegistry(
			javaServices = @JavaService(
					role = AdditionalMappingContributor.class,
					impl = NameColumnOrmXmlContributor.class
			)
	)
	@ServiceRegistry
	@DomainModel
	@SuppressWarnings("JUnitMalformedDeclaration")
	void name(DomainModelScope domainModelScope) {
		verifyOrmXmlContribution( domainModelScope, new NameColumnOrmXmlContributor() );
	}

	@Test
	@BootstrapServiceRegistry(
			javaServices = @JavaService(
					role = AdditionalMappingContributor.class,
					impl = UniqueColumnOrmXmlContributor.class
			)
	)
	@ServiceRegistry
	@DomainModel
	@SuppressWarnings("JUnitMalformedDeclaration")
	void unique(DomainModelScope domainModelScope) {
		verifyOrmXmlContribution( domainModelScope, new UniqueColumnOrmXmlContributor() );
	}

	@Test
	@BootstrapServiceRegistry(
			javaServices = @JavaService(
					role = AdditionalMappingContributor.class,
					impl = NullableColumnOrmXmlContributor.class
			)
	)
	@ServiceRegistry
	@DomainModel
	@SuppressWarnings("JUnitMalformedDeclaration")
	void nullable(DomainModelScope domainModelScope) {
		verifyOrmXmlContribution( domainModelScope, new NullableColumnOrmXmlContributor() );
	}

	@Test
	@BootstrapServiceRegistry(
			javaServices = @JavaService(
					role = AdditionalMappingContributor.class,
					impl = LengthColumnOrmXmlContributor.class
			)
	)
	@ServiceRegistry
	@DomainModel
	@SuppressWarnings("JUnitMalformedDeclaration")
	void length(DomainModelScope domainModelScope) {
		verifyOrmXmlContribution( domainModelScope, new LengthColumnOrmXmlContributor() );
	}

	@Test
	@BootstrapServiceRegistry(
			javaServices = @JavaService(
					role = AdditionalMappingContributor.class,
					impl = PrecisionColumnOrmXmlContributor.class
			)
	)
	@ServiceRegistry
	@DomainModel
	@SuppressWarnings("JUnitMalformedDeclaration")
	void precision(DomainModelScope domainModelScope) {
		verifyOrmXmlContribution( domainModelScope, new PrecisionColumnOrmXmlContributor() );
	}

	@Test
	@BootstrapServiceRegistry(
			javaServices = @JavaService(
					role = AdditionalMappingContributor.class,
					impl = ScaleColumnOrmXmlContributor.class
			)
	)
	@ServiceRegistry
	@DomainModel
	@SuppressWarnings("JUnitMalformedDeclaration")
	void scale(DomainModelScope domainModelScope) {
		verifyOrmXmlContribution( domainModelScope, new ScaleColumnOrmXmlContributor() );
	}

	void verifyOrmXmlContribution(DomainModelScope domainModelScope, ColumnOrmXmlContributor contributor) {
		final PersistentClass binding = domainModelScope.getDomainModel().getEntityBinding( Entity1.class.getName() );
		assertThat( binding ).isNotNull();
		assertThat( binding.getIdentifierProperty() ).isNotNull();
		assertThat( binding.getProperties() ).hasSize( 2 );
		assertThat( binding.getProperties().stream()
				.filter( property -> contributor.attributeName.equals( property.getName() ) )
				.findFirst().orElseThrow() )
				.satisfies( property -> assertColumn( property, contributor ) );
	}

	private void assertColumn(Property property, ColumnOrmXmlContributor contributor) {
		assertThat( property.getColumns() ).hasSize( 1 );
		assertThat( property.getColumns().get( 0 ) ).satisfies( column -> {
			assertThat( column.getName() ).isEqualTo( contributor.defaultName() );
			assertThat( column.isUnique() ).isEqualTo( contributor.defaultUnique() );
			assertThat( column.isNullable() ).isEqualTo( contributor.defaultNullable() );
			assertThat( column.getLength() ).isEqualTo( contributor.defaultLength() );
			assertThat( column.getPrecision() ).isEqualTo( contributor.defaultPrecision() );
			assertThat( column.getScale() ).isEqualTo( contributor.defaultScale() );
		} );
	}


	public static class Entity1 {
		private Integer id;
		private String name;

		private Double number;

		@SuppressWarnings("unused")
		protected Entity1() {
			// for use by Hibernate
		}

		public Entity1(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Double getNumber() {
			return number;
		}

		public void setNumber(Double number) {
			this.number = number;
		}
	}

	public static class NameColumnOrmXmlContributor extends ColumnOrmXmlContributor {

		public NameColumnOrmXmlContributor() {
			super( "name", "name", "something" );
		}

		@Override
		public String defaultName() {
			return "something";
		}
	}

	public static class UniqueColumnOrmXmlContributor extends ColumnOrmXmlContributor {

		public UniqueColumnOrmXmlContributor() {
			super( "name", "unique", true );
		}

		@Override
		public boolean defaultUnique() {
			return true;
		}
	}

	public static class NullableColumnOrmXmlContributor extends ColumnOrmXmlContributor {

		public NullableColumnOrmXmlContributor() {
			super( "name", "nullable", true );
		}
	}

	public static class LengthColumnOrmXmlContributor extends ColumnOrmXmlContributor {

		public LengthColumnOrmXmlContributor() {
			super( "name", "length", 512 );
		}

		@Override
		public int defaultLength() {
			return 512;
		}
	}

	public static class PrecisionColumnOrmXmlContributor extends ColumnOrmXmlContributor {

		public PrecisionColumnOrmXmlContributor() {
			super( "number", "precision", 5 );
		}

		@Override
		public Integer defaultPrecision() {
			return 5;
		}

		@Override
		public Integer defaultScale() {
			return 0;
		}
	}

	public static class ScaleColumnOrmXmlContributor extends ColumnOrmXmlContributor {

		public ScaleColumnOrmXmlContributor() {
			// scale without precision is ignored
			super( "number", "scale", 5 );
		}

		@Override
		public Integer defaultScale() {
			return null;
		}

		@Override
		public Integer defaultPrecision() {
			return null;
		}
	}

	public static abstract class ColumnOrmXmlContributor implements AdditionalMappingContributor {

		private final String attributeName;
		private final String columnAttribute;
		private final Object columnAttributeValue;

		protected ColumnOrmXmlContributor(String attributeName, String columnAttribute, Object columnAttributeValue) {
			this.attributeName = attributeName;
			this.columnAttribute = columnAttribute;
			this.columnAttributeValue = columnAttributeValue;
		}

		@Override
		public void contribute(
				AdditionalMappingContributions contributions,
				InFlightMetadataCollector metadata,
				ResourceStreamLocator resourceStreamLocator,
				MetadataBuildingContext buildingContext) {
			try ( final InputStream stream = new ByteArrayInputStream( String.format( Locale.ROOT,
					"<entity-mappings xmlns=\"http://www.hibernate.org/xsd/orm/mapping\" version=\"3.1\">\n" +
							"    <entity class=\"org.hibernate.orm.test.intg.AdditionalMappingContributorBasicColumnTests$Entity1\">\n" +
							"        <attributes>\n" +
							"            <id name=\"id\"/>\n" +
							"            <basic name=\"%s\">\n" +
							"                <column %s=\"%s\"/>\n" +
							"            </basic>\n" +
							"        </attributes>\n" +
							"    </entity>\n" +
							"</entity-mappings>", attributeName, columnAttribute, columnAttributeValue
			).getBytes( StandardCharsets.UTF_8 ) ) ) {

				contributions.contributeBinding( stream );
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}

		public String defaultName() {
			return attributeName;
		}

		public boolean defaultUnique() {
			return false;
		}

		public boolean defaultNullable() {
			return true;
		}

		public boolean defaultInsertable() {
			return true;
		}

		public boolean defaultUpdatable() {
			return true;
		}

		public String defaultColumnDefinition() {
			return "";
		}

		public String defaultTable() {
			return "";
		}

		public int defaultLength() {
			return 255;
		}

		public Integer defaultPrecision() {
			return null;
		}

		public Integer defaultScale() {
			return null;
		}
	}
}
