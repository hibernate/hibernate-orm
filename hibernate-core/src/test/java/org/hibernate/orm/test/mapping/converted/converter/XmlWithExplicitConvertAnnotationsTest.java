/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import java.net.MalformedURLException;
import java.util.Date;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import org.hibernate.Session;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertTrue;

/**
 * Jira HHH-8812 claims that explicit {@link jakarta.persistence.Convert} annotations are not processed when a orm.xml
 * file is used - specifically that the mixed case is not handled properly.
 *
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-8812" )
public class XmlWithExplicitConvertAnnotationsTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Entity1.class };
	}

	@Override
	protected String[] getXmlFiles() {
		return new String[] { "org/hibernate/test/converter/mixed.xml" };
	}

	// NOTE : essentially the same exact test as ExplicitDateConvertersTest, but here we will mix annotations and xml

	static boolean convertToDatabaseColumnCalled = false;
	static boolean convertToEntityAttributeCalled = false;

	private void resetFlags() {
		convertToDatabaseColumnCalled = false;
		convertToEntityAttributeCalled = false;
	}

	public static class LongToDateConverter implements AttributeConverter<Date,Long> {
		@Override
		public Long convertToDatabaseColumn(Date attribute) {
			convertToDatabaseColumnCalled = true;
			return attribute.getTime();
		}

		@Override
		public Date convertToEntityAttribute(Long dbData) {
			convertToEntityAttributeCalled = true;
			return new Date( dbData );
		}
	}

	@Entity( name = "Entity1" )
	public static class Entity1 {
		@Id
		private Integer id;
		private String name;
		@Convert( converter = LongToDateConverter.class )
		private Date theDate;

		public Entity1() {
		}

		public Entity1(Integer id, String name, Date theDate) {
			this.id = id;
			this.name = name;
			this.theDate = theDate;
		}
	}

	public static class TestEntityListener {
		@PrePersist
		@PreUpdate
		private void listen(Object entity) {
			System.out.println( "@PrePersist @PreUpdate listener event fired" );
		}
	}

	@Test
	public void testSimpleConvertUsage() throws MalformedURLException {
		final EntityPersister ep = sessionFactory().getMappingMetamodel().getEntityDescriptor(Entity1.class.getName());
		final Type theDatePropertyType = ep.getPropertyType( "theDate" );
		final ConvertedBasicTypeImpl type = assertTyping(
				ConvertedBasicTypeImpl.class,
				theDatePropertyType
		);
		final JpaAttributeConverter converter = (JpaAttributeConverter) type.getValueConverter();
		assertTrue( LongToDateConverter.class.isAssignableFrom( converter.getConverterJavaType().getJavaTypeClass() ) );

		resetFlags();

		Session session = openSession();
		session.getTransaction().begin();
		session.persist( new Entity1( 1, "1", new Date() ) );
		session.getTransaction().commit();
		session.close();

		assertTrue( convertToDatabaseColumnCalled );
		resetFlags();

		session = openSession();
		session.getTransaction().begin();
		session.get( Entity1.class, 1 );
		session.getTransaction().commit();
		session.close();

		assertTrue( convertToEntityAttributeCalled );

		session = openSession();
		session.getTransaction().begin();
		session.createQuery( "delete Entity1" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

}
