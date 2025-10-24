/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.spi.metadatabuildercontributor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(H2Dialect.class)
@JiraKey(value = "HHH-13040")
public class AttributeConverterMetadataBuilderContributorTest extends EntityManagerFactoryBasedFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Employee.class,
		};
	}

	public static class MetadataBuilderProvider implements SettingProvider.Provider<Object> {
		@Override
		public Object getSetting() {
			return null;
		}
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put(
				EntityManagerFactoryBuilderImpl.METADATA_BUILDER_CONTRIBUTOR,
				(MetadataBuilderContributor) metadataBuilder ->
						metadataBuilder.applyAttributeConverter( YearMonthAttributeConverter.class )
		);
	}

	final Employee employee = new Employee();

	@BeforeEach
	protected void setUp() {
		inTransaction( entityManager -> {
			employee.id = 1L;
			employee.username = "user@acme.com";
			employee.nextVacation = YearMonth.of( 2018, 12 );

			entityManager.persist( employee );
		} );
	}

	@Test
	public void test() {
		inTransaction( entityManager -> {
			Employee employee = entityManager.find( Employee.class, 1L );

			assertThat( employee.nextVacation ).isEqualTo( YearMonth.of( 2018, 12 ) );
		} );
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private Long id;

		@NaturalId
		private String username;

		@Column(name = "next_vacation", columnDefinition = "INTEGER")
		private YearMonth nextVacation;
	}

	@Converter(autoApply = true)
	public static class YearMonthAttributeConverter
			implements AttributeConverter<YearMonth, Integer> {

		@Override
		public Integer convertToDatabaseColumn(YearMonth attribute) {
			return (attribute.getYear() * 100) + attribute.getMonth().getValue();
		}

		@Override
		public YearMonth convertToEntityAttribute(Integer dbData) {
			return YearMonth.of( dbData / 100, dbData % 100 );
		}
	}

}
