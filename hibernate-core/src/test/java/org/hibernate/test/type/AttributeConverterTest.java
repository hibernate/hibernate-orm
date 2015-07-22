/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.io.Serializable;
import java.sql.Timestamp;
import java.sql.Types;
import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.AnnotationException;
import org.hibernate.IrrelevantEntity;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.tree.JavaConstantNode;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * Tests the principle of adding "AttributeConverter" to the mix of {@link org.hibernate.type.Type} resolution
 *
 * @author Steve Ebersole
 */
public class AttributeConverterTest extends BaseUnitTestCase {
	@Test
	public void testErrorInstantiatingConverterClass() {
		Configuration cfg = new Configuration();
		try {
			cfg.addAttributeConverter( BlowsUpConverter.class );
			fail( "expecting an exception" );
		}
		catch (AnnotationException e) {
			assertNotNull( e.getCause() );
			assertTyping( BlewUpException.class, e.getCause() );
		}
	}

	public static class BlewUpException extends RuntimeException {
	}

	public static class BlowsUpConverter implements AttributeConverter<String,String> {
		public BlowsUpConverter() {
			throw new BlewUpException();
		}

		@Override
		public String convertToDatabaseColumn(String attribute) {
			return null;
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			return null;
		}
	}

	@Test
	public void testBasicOperation() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr ).buildMetadata();
			SimpleValue simpleValue = new SimpleValue( metadata );
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
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testNonAutoApplyHandling() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( Tester.class )
					.getMetadataBuilder()
					.applyAttributeConverter( NotAutoAppliedConverter.class, false )
					.build();

			PersistentClass tester = metadata.getEntityBinding( Tester.class.getName() );
			Property nameProp = tester.getProperty( "name" );
			SimpleValue nameValue = (SimpleValue) nameProp.getValue();
			Type type = nameValue.getType();
			assertNotNull( type );
			if ( AttributeConverterTypeAdapter.class.isInstance( type ) ) {
				fail( "AttributeConverter with autoApply=false was auto applied" );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}

	}

	@Test
	public void testBasicConverterApplication() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( Tester.class )
					.getMetadataBuilder()
					.applyAttributeConverter( StringClobConverter.class, true )
					.build();

			PersistentClass tester = metadata.getEntityBinding( Tester.class.getName() );
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
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8462")
	public void testBasicOrmXmlConverterApplication() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( Tester.class )
					.addURL( ConfigHelper.findAsResource( "org/hibernate/test/type/orm.xml" ) )
					.getMetadataBuilder()
					.build();

			PersistentClass tester = metadata.getEntityBinding( Tester.class.getName() );
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
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testBasicConverterDisableApplication() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( Tester2.class )
					.getMetadataBuilder()
					.applyAttributeConverter( StringClobConverter.class, true )
					.build();

			PersistentClass tester = metadata.getEntityBinding( Tester2.class.getName() );
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
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testBasicUsage() {
		Configuration cfg = new Configuration();
		cfg.addAttributeConverter( IntegerToVarcharConverter.class, false );
		cfg.addAnnotatedClass( Tester4.class );
		cfg.setProperty( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		cfg.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );

		SessionFactory sf = cfg.buildSessionFactory();

		try {
			Session session = sf.openSession();
			session.beginTransaction();
			session.save( new Tester4( 1L, "steve", 200 ) );
			session.getTransaction().commit();
			session.close();

			sf.getStatistics().clear();
			session = sf.openSession();
			session.beginTransaction();
			session.get( Tester4.class, 1L );
			session.getTransaction().commit();
			session.close();
			assertEquals( 0, sf.getStatistics().getEntityUpdateCount() );

			session = sf.openSession();
			session.beginTransaction();
			Tester4 t4 = (Tester4) session.get( Tester4.class, 1L );
			t4.code = 300;
			session.getTransaction().commit();
			session.close();

			session = sf.openSession();
			session.beginTransaction();
			t4 = (Tester4) session.get( Tester4.class, 1L );
			assertEquals( 300, t4.code.longValue() );
			session.delete( t4 );
			session.getTransaction().commit();
			session.close();
		}
		finally {
			sf.close();
		}
	}

	@Test
	public void testBasicTimestampUsage() {
		Configuration cfg = new Configuration();
		cfg.addAttributeConverter( InstantConverter.class, false );
		cfg.addAnnotatedClass( IrrelevantInstantEntity.class );
		cfg.setProperty( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		cfg.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );

		SessionFactory sf = cfg.buildSessionFactory();

		try {
			Session session = sf.openSession();
			session.beginTransaction();
			session.save( new IrrelevantInstantEntity( 1L ) );
			session.getTransaction().commit();
			session.close();

			sf.getStatistics().clear();
			session = sf.openSession();
			session.beginTransaction();
			IrrelevantInstantEntity e = (IrrelevantInstantEntity) session.get( IrrelevantInstantEntity.class, 1L );
			session.getTransaction().commit();
			session.close();
			assertEquals( 0, sf.getStatistics().getEntityUpdateCount() );

			session = sf.openSession();
			session.beginTransaction();
			session.delete( e );
			session.getTransaction().commit();
			session.close();
		}
		finally {
			sf.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8866")
	public void testEnumConverter() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.build();

		try {
			MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( EntityWithConvertibleField.class )
					.getMetadataBuilder()
					.applyAttributeConverter( ConvertibleEnumConverter.class, true )
					.build();

			// first lets validate that the converter was applied...
			PersistentClass tester = metadata.getEntityBinding( EntityWithConvertibleField.class.getName() );
			Property nameProp = tester.getProperty( "convertibleEnum" );
			SimpleValue nameValue = (SimpleValue) nameProp.getValue();
			Type type = nameValue.getType();
			assertNotNull( type );
			assertTyping( BasicType.class, type );
			if ( !AttributeConverterTypeAdapter.class.isInstance( type ) ) {
				fail( "AttributeConverter not applied" );
			}
			AbstractStandardBasicType basicType = assertTyping( AbstractStandardBasicType.class, type );
			assertTyping( EnumJavaTypeDescriptor.class, basicType.getJavaTypeDescriptor() );
			assertEquals( Types.VARCHAR, basicType.getSqlTypeDescriptor().getSqlType() );

			// then lets build the SF and verify its use...
			final SessionFactory sf = metadata.buildSessionFactory();
			try {
				Session s = sf.openSession();
				s.getTransaction().begin();
				EntityWithConvertibleField entity = new EntityWithConvertibleField();
				entity.setId( "ID" );
				entity.setConvertibleEnum( ConvertibleEnum.VALUE );
				String entityID = entity.getId();
				s.persist( entity );
				s.getTransaction().commit();
				s.close();

				s = sf.openSession();
				s.beginTransaction();
				entity = (EntityWithConvertibleField) s.load( EntityWithConvertibleField.class, entityID );
				assertEquals( ConvertibleEnum.VALUE, entity.getConvertibleEnum() );
				s.getTransaction().commit();
				s.close();

				JavaConstantNode javaConstantNode = new JavaConstantNode();
				javaConstantNode.setExpectedType( type );
				javaConstantNode.setSessionFactory( (SessionFactoryImplementor) sf );
				javaConstantNode.setText( "org.hibernate.test.type.AttributeConverterTest$ConvertibleEnum.VALUE" );
				final String outcome = javaConstantNode.getRenderText( (SessionFactoryImplementor) sf );
				assertEquals( "'VALUE'", outcome );

				s = sf.openSession();
				s.beginTransaction();
				s.createQuery( "FROM EntityWithConvertibleField e where e.convertibleEnum = org.hibernate.test.type.AttributeConverterTest$ConvertibleEnum.VALUE" )
						.list();
				s.getTransaction().commit();
				s.close();

				s = sf.openSession();
				s.beginTransaction();
				s.delete( entity );
				s.getTransaction().commit();
				s.close();
			}
			finally {
				try {
					sf.close();
				}
				catch (Exception ignore) {
				}
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
	
	

	// Entity declarations used in the test ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Entity(name = "T1")
	@SuppressWarnings("UnusedDeclaration")
	public static class Tester {
		@Id
		private Long id;
		private String name;

		public Tester() {
		}

		public Tester(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "T2")
	@SuppressWarnings("UnusedDeclaration")
	public static class Tester2 {
		@Id
		private Long id;
		@Convert(disableConversion = true)
		private String name;
	}

	@Entity(name = "T3")
	@SuppressWarnings("UnusedDeclaration")
	public static class Tester3 {
		@Id
		private Long id;
		@org.hibernate.annotations.Type( type = "string" )
		@Convert(disableConversion = true)
		private String name;
	}

	@Entity(name = "T4")
	@SuppressWarnings("UnusedDeclaration")
	public static class Tester4 {
		@Id
		private Long id;
		private String name;
		@Convert( converter = IntegerToVarcharConverter.class )
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
	@Table(name = "irrelevantInstantEntity")
	@SuppressWarnings("UnusedDeclaration")
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

	public static enum ConvertibleEnum {
		VALUE,
		DEFAULT;

		public String convertToString() {
			switch ( this ) {
				case VALUE: {
					return "VALUE";
				}
				default: {
					return "DEFAULT";
				}
			}
		}
	}

	@Converter(autoApply = true)
	public static class ConvertibleEnumConverter implements AttributeConverter<ConvertibleEnum, String> {
		@Override
		public String convertToDatabaseColumn(ConvertibleEnum attribute) {
			return attribute.convertToString();
		}

		@Override
		public ConvertibleEnum convertToEntityAttribute(String dbData) {
			return ConvertibleEnum.valueOf( dbData );
		}
	}

	@Entity( name = "EntityWithConvertibleField" )
	@Table( name = "EntityWithConvertibleField" )
	public static class EntityWithConvertibleField {
		private String id;
		private ConvertibleEnum convertibleEnum;

		@Id
		@Column(name = "id")
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Column(name = "testEnum")

		public ConvertibleEnum getConvertibleEnum() {
			return convertibleEnum;
		}

		public void setConvertibleEnum(ConvertibleEnum convertibleEnum) {
			this.convertibleEnum = convertibleEnum;
		}
	}


	// Converter declarations used in the test ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Converter(autoApply = false)
	public static class NotAutoAppliedConverter implements AttributeConverter<String,String> {
		@Override
		public String convertToDatabaseColumn(String attribute) {
			throw new IllegalStateException( "AttributeConverter should not have been applied/called" );
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			throw new IllegalStateException( "AttributeConverter should not have been applied/called" );
		}
	}

	@Converter( autoApply = true )
	public static class IntegerToVarcharConverter implements AttributeConverter<Integer,String> {
		@Override
		public String convertToDatabaseColumn(Integer attribute) {
			return attribute == null ? null : attribute.toString();
		}

		@Override
		public Integer convertToEntityAttribute(String dbData) {
			return dbData == null ? null : Integer.valueOf( dbData );
		}
	}


	@Converter( autoApply = true )
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
