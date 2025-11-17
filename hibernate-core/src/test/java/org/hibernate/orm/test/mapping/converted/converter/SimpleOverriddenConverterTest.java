/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.sql.Types;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;

/**
 * Tests MappedSuperclass/Entity overriding of Convert definitions
 *
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {SimpleOverriddenConverterTest.Super.class, SimpleOverriddenConverterTest.Sub.class})
@SessionFactory(exportSchema = false)
public class SimpleOverriddenConverterTest {

	/**
	 * Test outcome of annotations exclusively.
	 */
	@Test
	public void testSimpleConvertOverrides(SessionFactoryScope scope) {
		final EntityPersister ep = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor(Sub.class.getName());
		final JdbcTypeRegistry jdbcTypeRegistry = scope.getSessionFactory().getTypeConfiguration().getJdbcTypeRegistry();

		BasicType<?> type = (BasicType<?>) ep.getPropertyType( "it" );
		assertTyping( StringJavaType.class, type.getJavaTypeDescriptor() );
		assertTyping( jdbcTypeRegistry.getDescriptor( Types.VARCHAR ).getClass(), type.getJdbcType() );
	}

	@MappedSuperclass
	public static class Super {
		@Id
		public Integer id;
		@Convert(converter = SillyStringConverter.class)
		public String it;
	}

	@Entity(name = "Sub")
	@Convert( attributeName = "it", disableConversion = true )
	public static class Sub extends Super {

	}

}
