/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.type;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.sql.Timestamp;
import java.sql.Types;

import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.IrrelevantEntity;
import org.hibernate.Session;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.Test;

/**
 * Tests the principle of adding "AttributeConverter" to the mix of {@link org.hibernate.type.Type} resolution
 *
 * @author Steve Ebersole
 */
public class AttributeConverterTest extends BaseCoreFunctionalTestCase {
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				EntityWithConvertibleField.class,
				Tester.class,
				Tester2.class,
				Tester3.class,
				Tester4.class,
				IrrelevantInstantEntity.class
		};
	}

	protected void beforeConfiguration() {
		configuration().addAttributeConverter( NotAutoAppliedConverter.class, false );
		configuration().addAttributeConverter( EnumConverter.class, true );
		configuration().addAttributeConverter( StringClobConverter.class, true );
		configuration().addAttributeConverter( IntegerToVarcharConverter.class, false );
		configuration().addAttributeConverter( InstantConverter.class, false );
		configuration().setProperty( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testBasicOperation() {
		SimpleValue simpleValue = new SimpleValue( configuration().createMappings() );
		simpleValue.setJpaAttributeConverterDefinition(
				new AttributeConverterDefinition( new StringClobConverter(), true )
		);
		simpleValue.setTypeUsingReflection( IrrelevantEntity.class.getName(), "name" );

		Type type = simpleValue.getType();
		assertNotNull( type );
		if ( !AttributeConverterTypeAdapter.class.isInstance( type ) ) {
			fail( "AttributeConverter not applied" );
		}
		AbstractStandardBasicType basicType = assertTyping( AbstractStandardBasicType.class, type );
		assertSame( StringTypeDescriptor.INSTANCE, basicType.getJavaTypeDescriptor() );
		assertEquals( Types.CLOB, basicType.getSqlTypeDescriptor().getSqlType() );
	}

	@Test
	public void testNonAutoApplyHandling() {
		PersistentClass tester = configuration().getClassMapping( Tester3.class.getName() );
		Property nameProp = tester.getProperty( "name" );
		SimpleValue nameValue = (SimpleValue) nameProp.getValue();
		Type type = nameValue.getType();
		assertNotNull( type );
		if ( AttributeConverterTypeAdapter.class.isInstance( type ) ) {
			fail( "AttributeConverter with autoApply=false was auto applied" );
		}
	}

	@Test
	public void testBasicConverterApplication() {
		PersistentClass tester = configuration().getClassMapping( Tester.class.getName() );
		Property nameProp = tester.getProperty( "name" );
		SimpleValue nameValue = (SimpleValue) nameProp.getValue();
		Type type = nameValue.getType();
		assertNotNull( type );
		assertTyping( BasicType.class, type );
		if ( !AttributeConverterTypeAdapter.class.isInstance( type ) ) {
			fail( "AttributeConverter not applied" );
		}
		AbstractStandardBasicType basicType = assertTyping( AbstractStandardBasicType.class, type );
		assertSame( StringTypeDescriptor.INSTANCE, basicType.getJavaTypeDescriptor() );
		assertEquals( Types.CLOB, basicType.getSqlTypeDescriptor().getSqlType() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8462")
	public void testBasicOrmXmlConverterApplication() {
		PersistentClass tester = configuration().getClassMapping( Tester.class.getName() );
		Property nameProp = tester.getProperty( "name" );
		SimpleValue nameValue = (SimpleValue) nameProp.getValue();
		Type type = nameValue.getType();
		assertNotNull( type );
		if ( !AttributeConverterTypeAdapter.class.isInstance( type ) ) {
			fail( "AttributeConverter not applied" );
		}
		AttributeConverterTypeAdapter basicType = assertTyping( AttributeConverterTypeAdapter.class, type );
		assertSame( StringTypeDescriptor.INSTANCE, basicType.getJavaTypeDescriptor() );
		assertEquals( Types.CLOB, basicType.getSqlTypeDescriptor().getSqlType() );
	}

	@Test
	public void testBasicConverterDisableApplication() {
		PersistentClass tester = configuration().getClassMapping( Tester2.class.getName() );
		Property nameProp = tester.getProperty( "name" );
		SimpleValue nameValue = (SimpleValue) nameProp.getValue();
		Type type = nameValue.getType();
		assertNotNull( type );
		if ( AttributeConverterTypeAdapter.class.isInstance( type ) ) {
			fail( "AttributeConverter applied (should not have been)" );
		}
		AbstractStandardBasicType basicType = assertTyping( AbstractStandardBasicType.class, type );
		assertSame( StringTypeDescriptor.INSTANCE, basicType.getJavaTypeDescriptor() );
		assertEquals( Types.VARCHAR, basicType.getSqlTypeDescriptor().getSqlType() );
	}

	@Test
	public void testBasicUsage() {
		Session session = openSession();
		session.beginTransaction();
		session.save( new Tester4( 1L, "steve", 200 ) );
		session.getTransaction().commit();
		session.close();

		sessionFactory().getStatistics().clear();
		session = openSession();
		session.beginTransaction();
		session.get( Tester4.class, 1L );
		session.getTransaction().commit();
		session.close();
		assertEquals( 0, sessionFactory().getStatistics().getEntityUpdateCount() );

		session = openSession();
		session.beginTransaction();
		Tester4 t4 = (Tester4) session.get( Tester4.class, 1L );
		t4.code = 300;
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		t4 = (Tester4) session.get( Tester4.class, 1L );
		assertEquals( 300, t4.code.longValue() );
		session.delete( t4 );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testBasicTimestampUsage() {
		Session session = openSession();
		session.beginTransaction();
		session.save( new IrrelevantInstantEntity( 1L ) );
		session.getTransaction().commit();
		session.close();

		sessionFactory().getStatistics().clear();
		session = openSession();
		session.beginTransaction();
		IrrelevantInstantEntity e = (IrrelevantInstantEntity) session.get( IrrelevantInstantEntity.class, 1L );
		session.getTransaction().commit();
		session.close();
		assertEquals( 0, sessionFactory().getStatistics().getEntityUpdateCount() );

		session = openSession();
		session.beginTransaction();
		session.delete( e );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8866")
	public void testEnumConverter() {
		Session s = openSession();
		s.getTransaction().begin();
		EntityWithConvertibleField entity = new EntityWithConvertibleField();
		entity.setId( "ID" );
		entity.setTestEnum( ConvertibleEnum.VALUE );
		String entityID = entity.getId();

		s.persist( entity );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();

		s.beginTransaction();
		entity = (EntityWithConvertibleField) s.load( EntityWithConvertibleField.class, entityID );
		assertEquals( ConvertibleEnum.VALUE, entity.getTestEnum() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8866")
	public void testHqlQueryEnumConverter() {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery(
				"SELECT ewcf " +
						"FROM EntityWithConvertibleField ewcf " +
						"WHERE ewcf.testEnum = org.hibernate.test.type.ConvertibleEnum.VALUE"
		);
		s.getTransaction().commit();
		s.close();
	}


	// Entity declarations used in the test ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Entity(name = "T1")
	public static class Tester {
		@Id
		private Long id;
		@Convert(converter = StringClobConverter.class)
		private String name;

		public Tester() {
		}

		public Tester(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "T2")
	public static class Tester2 {
		@Id
		private Long id;
		@Convert(disableConversion = true)
		private String name;
	}

	@Entity(name = "T3")
	public static class Tester3 {
		@Id
		private Long id;
		@org.hibernate.annotations.Type(type = "string")
		@Convert(disableConversion = true)
		private String name;
	}

	@Entity(name = "T4")
	public static class Tester4 {
		@Id
		private Long id;
		private String name;
		@Convert(converter = IntegerToVarcharConverter.class)
		private Integer code;

		public Tester4() {
		}

		public Tester4(Long id, String name, Integer code) {
			this.id = id;
			this.name = name;
			this.code = code;
		}
	}

	// This class is for mimicking an Instant from Java 8, which a converter might convert to a java.sql.Timestamp
	public static class Instant implements Serializable {
		private static final long serialVersionUID = 1L;

		private long javaMillis;

		public Instant(long javaMillis) {
			this.javaMillis = javaMillis;
		}

		public long toJavaMillis() {
			return javaMillis;
		}

		public static Instant fromJavaMillis(long javaMillis) {
			return new Instant( javaMillis );
		}

		public static Instant now() {
			return new Instant( System.currentTimeMillis() );
		}
	}

	@Entity
	public static class IrrelevantInstantEntity {
		@Id
		private Long id;
		private Instant dateCreated;

		public IrrelevantInstantEntity() {
		}

		public IrrelevantInstantEntity(Long id) {
			this( id, Instant.now() );
		}

		public IrrelevantInstantEntity(Long id, Instant dateCreated) {
			this.id = id;
			this.dateCreated = dateCreated;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Instant getDateCreated() {
			return dateCreated;
		}

		public void setDateCreated(Instant dateCreated) {
			this.dateCreated = dateCreated;
		}
	}


	// Converter declarations used in the test ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Converter(autoApply = false)
	public static class NotAutoAppliedConverter implements AttributeConverter<String, String> {
		@Override
		public String convertToDatabaseColumn(String attribute) {
			throw new IllegalStateException( "AttributeConverter should not have been applied/called" );
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			throw new IllegalStateException( "AttributeConverter should not have been applied/called" );
		}
	}

	@Converter(autoApply = true)
	public static class IntegerToVarcharConverter implements AttributeConverter<Integer, String> {
		@Override
		public String convertToDatabaseColumn(Integer attribute) {
			return attribute == null ? null : attribute.toString();
		}

		@Override
		public Integer convertToEntityAttribute(String dbData) {
			return dbData == null ? null : Integer.valueOf( dbData );
		}
	}


	@Converter(autoApply = true)
	public static class InstantConverter implements AttributeConverter<Instant, Timestamp> {
		@Override
		public Timestamp convertToDatabaseColumn(Instant attribute) {
			return new Timestamp( attribute.toJavaMillis() );
		}

		@Override
		public Instant convertToEntityAttribute(Timestamp dbData) {
			return Instant.fromJavaMillis( dbData.getTime() );
		}
	}
}
