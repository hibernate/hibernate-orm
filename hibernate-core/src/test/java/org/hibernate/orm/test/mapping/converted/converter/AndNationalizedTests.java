/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Nationalized;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test the combination of @Nationalized and @Convert
 *
 * @author Steve Ebersole
 */
@SkipForDialect(value = DB2Dialect.class, comment = "DB2 jdbc driver doesn't support setNString")
public class AndNationalizedTests extends BaseUnitTestCase {
	@Test
	@JiraKey( value = "HHH-9599")
	public void basicTest() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		try {
			Metadata metadata = new MetadataSources( ssr ).addAnnotatedClass( TestEntity.class ).buildMetadata();
			( (MetadataImplementor) metadata ).orderColumns( false );
			( (MetadataImplementor) metadata ).validate();

			final PersistentClass entityBinding = metadata.getEntityBinding( TestEntity.class.getName() );
			final Dialect dialect = metadata.getDatabase().getDialect();
			assertEquals(
					dialect.getNationalizationSupport().getVarcharVariantCode(),
					entityBinding.getProperty( "name" ).getType().getSqlTypeCodes( metadata )[0]
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "TestEntity")
	@Table(name = "TestEntity")
	public static class TestEntity {
		@Id
		public Integer id;
		@Nationalized
		@Convert(converter = NameConverter.class)
		public Name name;
	}

	public static class Name {
		private final String text;

		public Name(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}
	}

	public static class NameConverter implements AttributeConverter<Name,String> {
		@Override
		public String convertToDatabaseColumn(Name attribute) {
			return attribute.getText();
		}

		@Override
		public Name convertToEntityAttribute(String dbData) {
			return new Name( dbData );
		}
	}
}
