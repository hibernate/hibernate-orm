/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel;

import jakarta.persistence.*;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

@SessionFactory
@BootstrapServiceRegistry(
		javaServices = @BootstrapServiceRegistry.JavaService(
				role = AdditionalMappingContributor.class,
				impl = AdditionalMappingContributorAttributeConverterRegistrationTest.Hibernate7AdditionalMappingContributor.class
		)
)
@DomainModel(annotatedClasses = {
		AdditionalMappingContributorAttributeConverterRegistrationTest.BooleanTestPOJO.class,
		AdditionalMappingContributorAttributeConverterRegistrationTest.TestConverter.class
})
@JiraKey("HHH-20199")
public class AdditionalMappingContributorAttributeConverterRegistrationTest {

	@Test
	public void emptyUnitTestMethodToVerifyStartUp(SessionFactoryScope scope) {
	}


	@Entity(name = "BooleanTestPOJO")
	public static class BooleanTestPOJO {

		@Id
		@Column
		private Long id;
		@Column
		private Boolean test;

		@Column
		private String code;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Boolean getTest() {
			return test;
		}

		public void setTest(Boolean test) {
			this.test = test;
		}

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}

	@Converter
	public static class TestConverter implements AttributeConverter<Boolean, String> {

		public TestConverter() {

		}

		@Override
		public String convertToDatabaseColumn(Boolean attribute) {
			if (attribute == null) {
				return null;
			}
			return attribute ? "Y" : "N";
		}

		@Override
		public Boolean convertToEntityAttribute(String dbData) {
			if (dbData == null) {
				return null;
			}
			return "Y".equals(dbData);
		}
	}

	@Entity
	public static class TestAdditionalMappingContributorEntity {
		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	public static class Hibernate7AdditionalMappingContributor implements AdditionalMappingContributor {

		@Override
		public void contribute(AdditionalMappingContributions contributions, InFlightMetadataCollector metadata, ResourceStreamLocator resourceStreamLocator, MetadataBuildingContext buildingContext) {
// Skip if already registered
			if (metadata.getEntityBinding(TestAdditionalMappingContributorEntity.class.getName()) != null) {
				return;
			}
			contributions.contributeEntity(buildingContext.getBootstrapContext().getClassLoaderAccess().classForName(TestAdditionalMappingContributorEntity.class.getName()));
		}
	}

}
