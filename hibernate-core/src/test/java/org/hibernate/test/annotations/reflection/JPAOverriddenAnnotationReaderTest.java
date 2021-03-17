/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.reflection;

import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.hibernate.annotations.Columns;
import org.hibernate.cfg.EJB3DTDEntityResolver;
import org.hibernate.cfg.annotations.reflection.JPAOverriddenAnnotationReader;
import org.hibernate.cfg.annotations.reflection.XMLContext;
import org.hibernate.internal.util.xml.ErrorLogger;
import org.hibernate.internal.util.xml.XMLHelper;

import org.hibernate.testing.boot.BootstrapContextImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.boot.ClassLoaderServiceTestingImpl;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;

import javax.persistence.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * @author Emmanuel Bernard
 */
public class JPAOverriddenAnnotationReaderTest extends BaseUnitTestCase {
	@Test
	public void testMappedSuperclassAnnotations() throws Exception {
		XMLContext context = buildContext(
				"org/hibernate/test/annotations/reflection/metadata-complete.xml"
		);
		JPAOverriddenAnnotationReader reader = new JPAOverriddenAnnotationReader( Organization.class, context, BootstrapContextImpl.INSTANCE );
		assertTrue( reader.isAnnotationPresent( MappedSuperclass.class ) );
	}

	@Test
	public void testEntityRelatedAnnotations() throws Exception {
		XMLContext context = buildContext( "org/hibernate/test/annotations/reflection/orm.xml" );
		JPAOverriddenAnnotationReader reader = new JPAOverriddenAnnotationReader( Administration.class, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( Entity.class ) );
		assertEquals(
				"Default value in xml entity should not override @Entity.name", "JavaAdministration",
				reader.getAnnotation( Entity.class ).name()
		);
		assertNotNull( reader.getAnnotation( Table.class ) );
		assertEquals( "@Table not overridden", "tbl_admin", reader.getAnnotation( Table.class ).name() );
		assertEquals( "Default schema not overridden", "myschema", reader.getAnnotation( Table.class ).schema() );
		assertEquals(
				"Proper @Table.uniqueConstraints", 2,
				reader.getAnnotation( Table.class ).uniqueConstraints()[0].columnNames().length
		);
		String columnName = reader.getAnnotation( Table.class ).uniqueConstraints()[0].columnNames()[0];
		assertTrue(
				"Proper @Table.uniqueConstraints", "firstname".equals( columnName ) || "lastname".equals( columnName )
		);
		assertNull( "Both Java and XML used", reader.getAnnotation( SecondaryTable.class ) );
		assertNotNull( "XML does not work", reader.getAnnotation( SecondaryTables.class ) );
		SecondaryTable[] tables = reader.getAnnotation( SecondaryTables.class ).value();
		assertEquals( 1, tables.length );
		assertEquals( "admin2", tables[0].name() );
		assertEquals( "unique constraints ignored", 1, tables[0].uniqueConstraints().length );
		assertEquals( "pk join column ignored", 1, tables[0].pkJoinColumns().length );
		assertEquals( "pk join column ignored", "admin_id", tables[0].pkJoinColumns()[0].name() );
		assertNotNull( "Sequence Overriding not working", reader.getAnnotation( SequenceGenerator.class ) );
		assertEquals(
				"wrong sequence name", "seqhilo", reader.getAnnotation( SequenceGenerator.class ).sequenceName()
		);
		assertEquals( "default fails", 50, reader.getAnnotation( SequenceGenerator.class ).allocationSize() );
		assertNotNull( "TableOverriding not working", reader.getAnnotation( TableGenerator.class ) );
		assertEquals( "wrong tble name", "tablehilo", reader.getAnnotation( TableGenerator.class ).table() );
		assertEquals( "no schema overriding", "myschema", reader.getAnnotation( TableGenerator.class ).schema() );

		reader = new JPAOverriddenAnnotationReader( Match.class, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( Table.class ) );
		assertEquals(
				"Java annotation not taken into account", "matchtable", reader.getAnnotation( Table.class ).name()
		);
		assertEquals(
				"Java annotation not taken into account", "matchschema", reader.getAnnotation( Table.class ).schema()
		);
		assertEquals( "Overriding not taken into account", "mycatalog", reader.getAnnotation( Table.class ).catalog() );
		assertNotNull( "SecondaryTable swallowed", reader.getAnnotation( SecondaryTables.class ) );
		assertEquals(
				"Default schema not taken into account", "myschema",
				reader.getAnnotation( SecondaryTables.class ).value()[0].schema()
		);
		assertNotNull( reader.getAnnotation( Inheritance.class ) );
		assertEquals(
				"inheritance strategy not overriden", InheritanceType.JOINED,
				reader.getAnnotation( Inheritance.class ).strategy()
		);
		assertNotNull( "NamedQuery not overriden", reader.getAnnotation( NamedQueries.class ) );
		assertEquals( "No deduplication", 3, reader.getAnnotation( NamedQueries.class ).value().length );
		assertEquals(
				"deduplication kept the Java version", 1,
				reader.getAnnotation( NamedQueries.class ).value()[1].hints().length
		);
		assertEquals(
				"org.hibernate.timeout", reader.getAnnotation( NamedQueries.class ).value()[1].hints()[0].name()
		);
		assertNotNull( "NamedNativeQuery not overriden", reader.getAnnotation( NamedNativeQueries.class ) );
		assertEquals( "No deduplication", 3, reader.getAnnotation( NamedNativeQueries.class ).value().length );
		assertEquals(
				"deduplication kept the Java version", 1,
				reader.getAnnotation( NamedNativeQueries.class ).value()[1].hints().length
		);
		assertEquals(
				"org.hibernate.timeout", reader.getAnnotation( NamedNativeQueries.class ).value()[1].hints()[0].name()
		);
		assertNotNull( reader.getAnnotation( SqlResultSetMappings.class ) );
		assertEquals(
				"competitor1Point", reader.getAnnotation( SqlResultSetMappings.class ).value()[0].columns()[0].name()
		);
		assertEquals(
				"competitor1Point",
				reader.getAnnotation( SqlResultSetMappings.class ).value()[0].entities()[0].fields()[0].column()
		);
		assertNotNull( reader.getAnnotation( ExcludeSuperclassListeners.class ) );
		assertNotNull( reader.getAnnotation( ExcludeDefaultListeners.class ) );

		reader = new JPAOverriddenAnnotationReader( Competition.class, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( MappedSuperclass.class ) );

		reader = new JPAOverriddenAnnotationReader( TennisMatch.class, context, BootstrapContextImpl.INSTANCE );
		assertNull( "Mutualize PKJC into PKJCs", reader.getAnnotation( PrimaryKeyJoinColumn.class ) );
		assertNotNull( reader.getAnnotation( PrimaryKeyJoinColumns.class ) );
		assertEquals(
				"PrimaryKeyJoinColumn overrden", "id",
				reader.getAnnotation( PrimaryKeyJoinColumns.class ).value()[0].name()
		);
		assertNotNull( reader.getAnnotation( AttributeOverrides.class ) );
		assertEquals( "Wrong deduplication", 3, reader.getAnnotation( AttributeOverrides.class ).value().length );
		assertEquals(
				"Wrong priority (XML vs java annotations)", "fld_net",
				reader.getAnnotation( AttributeOverrides.class ).value()[0].column().name()
		);
		assertEquals(
				"Column mapping", 2, reader.getAnnotation( AttributeOverrides.class ).value()[1].column().scale()
		);
		assertEquals(
				"Column mapping", true, reader.getAnnotation( AttributeOverrides.class ).value()[1].column().unique()
		);
		assertNotNull( reader.getAnnotation( AssociationOverrides.class ) );
		assertEquals( "no XML processing", 1, reader.getAnnotation( AssociationOverrides.class ).value().length );
		assertEquals(
				"wrong xml processing", "id",
				reader.getAnnotation( AssociationOverrides.class ).value()[0].joinColumns()[0].referencedColumnName()
		);


		reader = new JPAOverriddenAnnotationReader( SocialSecurityPhysicalAccount.class, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( IdClass.class ) );
		assertEquals( "id-class not used", SocialSecurityNumber.class, reader.getAnnotation( IdClass.class ).value() );
		assertEquals(
				"discriminator-value not used", "Physical", reader.getAnnotation( DiscriminatorValue.class ).value()
		);
		assertNotNull( "discriminator-column not used", reader.getAnnotation( DiscriminatorColumn.class ) );
		assertEquals(
				"discriminator-column.name default value broken", "DTYPE",
				reader.getAnnotation( DiscriminatorColumn.class ).name()
		);
		assertEquals(
				"discriminator-column.length broken", 34, reader.getAnnotation( DiscriminatorColumn.class ).length()
		);
	}

	@Test
	public void testEntityRelatedAnnotationsMetadataComplete() throws Exception {
		XMLContext context = buildContext(
				"org/hibernate/test/annotations/reflection/metadata-complete.xml"
		);
		JPAOverriddenAnnotationReader reader = new JPAOverriddenAnnotationReader( Administration.class, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( Entity.class ) );
		assertEquals(
				"Metadata complete should ignore java annotations", "", reader.getAnnotation( Entity.class ).name()
		);
		assertNotNull( reader.getAnnotation( Table.class ) );
		assertEquals( "@Table should not be used", "", reader.getAnnotation( Table.class ).name() );
		assertEquals( "Default schema not overriden", "myschema", reader.getAnnotation( Table.class ).schema() );

		reader = new JPAOverriddenAnnotationReader( Match.class, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( Table.class ) );
		assertEquals( "@Table should not be used", "", reader.getAnnotation( Table.class ).name() );
		assertEquals( "Overriding not taken into account", "myschema", reader.getAnnotation( Table.class ).schema() );
		assertEquals( "Overriding not taken into account", "mycatalog", reader.getAnnotation( Table.class ).catalog() );
		assertNull( "Ignore Java annotation", reader.getAnnotation( SecondaryTable.class ) );
		assertNull( "Ignore Java annotation", reader.getAnnotation( SecondaryTables.class ) );
		assertNull( "Ignore Java annotation", reader.getAnnotation( Inheritance.class ) );
		assertNull( reader.getAnnotation( NamedQueries.class ) );
		assertNull( reader.getAnnotation( NamedNativeQueries.class ) );

		reader = new JPAOverriddenAnnotationReader( TennisMatch.class, context, BootstrapContextImpl.INSTANCE );
		assertNull( reader.getAnnotation( PrimaryKeyJoinColumn.class ) );
		assertNull( reader.getAnnotation( PrimaryKeyJoinColumns.class ) );

		reader = new JPAOverriddenAnnotationReader( Competition.class, context, BootstrapContextImpl.INSTANCE );
		assertNull( reader.getAnnotation( MappedSuperclass.class ) );

		reader = new JPAOverriddenAnnotationReader( SocialSecurityMoralAccount.class, context, BootstrapContextImpl.INSTANCE );
		assertNull( reader.getAnnotation( IdClass.class ) );
		assertNull( reader.getAnnotation( DiscriminatorValue.class ) );
		assertNull( reader.getAnnotation( DiscriminatorColumn.class ) );
		assertNull( reader.getAnnotation( SequenceGenerator.class ) );
		assertNull( reader.getAnnotation( TableGenerator.class ) );
	}

	@Test
	public void testIdRelatedAnnotations() throws Exception {
		XMLContext context = buildContext( "org/hibernate/test/annotations/reflection/orm.xml" );
		Method method = Administration.class.getDeclaredMethod( "getId" );
		JPAOverriddenAnnotationReader reader = new JPAOverriddenAnnotationReader( method, context, BootstrapContextImpl.INSTANCE );
		assertNull( reader.getAnnotation( Id.class ) );
		assertNull( reader.getAnnotation( Column.class ) );
		Field field = Administration.class.getDeclaredField( "id" );
		reader = new JPAOverriddenAnnotationReader( field, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( Id.class ) );
		assertNotNull( reader.getAnnotation( GeneratedValue.class ) );
		assertEquals( GenerationType.SEQUENCE, reader.getAnnotation( GeneratedValue.class ).strategy() );
		assertEquals( "generator", reader.getAnnotation( GeneratedValue.class ).generator() );
		assertNotNull( reader.getAnnotation( SequenceGenerator.class ) );
		assertEquals( "seq", reader.getAnnotation( SequenceGenerator.class ).sequenceName() );
		assertNotNull( reader.getAnnotation( Columns.class ) );
		assertEquals( 1, reader.getAnnotation( Columns.class ).columns().length );
		assertEquals( "fld_id", reader.getAnnotation( Columns.class ).columns()[0].name() );
		assertNotNull( reader.getAnnotation( Temporal.class ) );
		assertEquals( TemporalType.DATE, reader.getAnnotation( Temporal.class ).value() );

		context = buildContext(
				"org/hibernate/test/annotations/reflection/metadata-complete.xml"
		);
		method = Administration.class.getDeclaredMethod( "getId" );
		reader = new JPAOverriddenAnnotationReader( method, context, BootstrapContextImpl.INSTANCE );
		assertNotNull(
				"Default access type when not defined in metadata complete should be property",
				reader.getAnnotation( Id.class )
		);
		field = Administration.class.getDeclaredField( "id" );
		reader = new JPAOverriddenAnnotationReader( field, context, BootstrapContextImpl.INSTANCE );
		assertNull(
				"Default access type when not defined in metadata complete should be property",
				reader.getAnnotation( Id.class )
		);

		method = BusTrip.class.getDeclaredMethod( "getId" );
		reader = new JPAOverriddenAnnotationReader( method, context, BootstrapContextImpl.INSTANCE );
		assertNull( reader.getAnnotation( EmbeddedId.class ) );
		field = BusTrip.class.getDeclaredField( "id" );
		reader = new JPAOverriddenAnnotationReader( field, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( EmbeddedId.class ) );
		assertNotNull( reader.getAnnotation( AttributeOverrides.class ) );
		assertEquals( 1, reader.getAnnotation( AttributeOverrides.class ).value().length );
	}

	@Test
	public void testBasicRelatedAnnotations() throws Exception {
		XMLContext context = buildContext(
				"org/hibernate/test/annotations/reflection/metadata-complete.xml"
		);
		Field field = BusTrip.class.getDeclaredField( "status" );
		JPAOverriddenAnnotationReader reader = new JPAOverriddenAnnotationReader( field, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( Enumerated.class ) );
		assertEquals( EnumType.STRING, reader.getAnnotation( Enumerated.class ).value() );
		assertEquals( false, reader.getAnnotation( Basic.class ).optional() );
		field = BusTrip.class.getDeclaredField( "serial" );
		reader = new JPAOverriddenAnnotationReader( field, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( Lob.class ) );
		assertEquals( "serialbytes", reader.getAnnotation( Columns.class ).columns()[0].name() );
		field = BusTrip.class.getDeclaredField( "terminusTime" );
		reader = new JPAOverriddenAnnotationReader( field, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( Temporal.class ) );
		assertEquals( TemporalType.TIMESTAMP, reader.getAnnotation( Temporal.class ).value() );
		assertEquals( FetchType.LAZY, reader.getAnnotation( Basic.class ).fetch() );

		field = BusTripPk.class.getDeclaredField( "busDriver" );
		reader = new JPAOverriddenAnnotationReader( field, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.isAnnotationPresent( Basic.class ) );
	}

	@Test
	public void testVersionRelatedAnnotations() throws Exception {
		XMLContext context = buildContext( "org/hibernate/test/annotations/reflection/orm.xml" );
		Method method = Administration.class.getDeclaredMethod( "getVersion" );
		JPAOverriddenAnnotationReader reader = new JPAOverriddenAnnotationReader( method, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( Version.class ) );

		Field field = Match.class.getDeclaredField( "version" );
		assertNotNull( reader.getAnnotation( Version.class ) );
	}

	@Test
	public void testTransientAndEmbeddedRelatedAnnotations() throws Exception {
		XMLContext context = buildContext( "org/hibernate/test/annotations/reflection/orm.xml" );

		Field field = Administration.class.getDeclaredField( "transientField" );
		JPAOverriddenAnnotationReader reader = new JPAOverriddenAnnotationReader( field, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( Transient.class ) );
		assertNull( reader.getAnnotation( Basic.class ) );

		field = Match.class.getDeclaredField( "playerASSN" );
		reader = new JPAOverriddenAnnotationReader( field, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( Embedded.class ) );
	}

	@Test
	public void testAssociationRelatedAnnotations() throws Exception {
		XMLContext context = buildContext( "org/hibernate/test/annotations/reflection/orm.xml" );

		Field field = Administration.class.getDeclaredField( "defaultBusTrip" );
		JPAOverriddenAnnotationReader reader = new JPAOverriddenAnnotationReader( field, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( OneToOne.class ) );
		assertNull( reader.getAnnotation( JoinColumns.class ) );
		assertNotNull( reader.getAnnotation( PrimaryKeyJoinColumns.class ) );
		assertEquals( "pk", reader.getAnnotation( PrimaryKeyJoinColumns.class ).value()[0].name() );
		assertEquals( 5, reader.getAnnotation( OneToOne.class ).cascade().length );
		assertEquals( FetchType.LAZY, reader.getAnnotation( OneToOne.class ).fetch() );
		assertEquals( "test", reader.getAnnotation( OneToOne.class ).mappedBy() );

		context = buildContext(
				"org/hibernate/test/annotations/reflection/metadata-complete.xml"
		);
		field = BusTrip.class.getDeclaredField( "players" );
		reader = new JPAOverriddenAnnotationReader( field, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( OneToMany.class ) );
		assertNotNull( reader.getAnnotation( JoinColumns.class ) );
		assertEquals( 2, reader.getAnnotation( JoinColumns.class ).value().length );
		assertEquals( "driver", reader.getAnnotation( JoinColumns.class ).value()[0].name() );
		assertNotNull( reader.getAnnotation( MapKey.class ) );
		assertEquals( "name", reader.getAnnotation( MapKey.class ).name() );

		field = BusTrip.class.getDeclaredField( "roads" );
		reader = new JPAOverriddenAnnotationReader( field, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( ManyToMany.class ) );
		assertNotNull( reader.getAnnotation( JoinTable.class ) );
		assertEquals( "bus_road", reader.getAnnotation( JoinTable.class ).name() );
		assertEquals( 2, reader.getAnnotation( JoinTable.class ).joinColumns().length );
		assertEquals( 1, reader.getAnnotation( JoinTable.class ).inverseJoinColumns().length );
		assertEquals( 2, reader.getAnnotation( JoinTable.class ).uniqueConstraints()[0].columnNames().length );
		assertNotNull( reader.getAnnotation( OrderBy.class ) );
		assertEquals( "maxSpeed", reader.getAnnotation( OrderBy.class ).value() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11924")
	public void testElementCollectionConverter() throws Exception {
		XMLContext context = buildContext( "org/hibernate/test/annotations/reflection/orm.xml" );

		Field field = Company.class.getDeclaredField( "organizations" );
		JPAOverriddenAnnotationReader reader = new JPAOverriddenAnnotationReader( field, context, BootstrapContextImpl.INSTANCE );
		assertNotNull( reader.getAnnotation( ElementCollection.class ) );
		assertNotNull( reader.getAnnotation( Converts.class ) );
		assertNotNull( reader.getAnnotation( Converts.class ).value() );
		assertTrue( reader.getAnnotation( Converts.class ).value().length == 1 );
		assertEquals(OrganizationConverter.class, reader.getAnnotation( Converts.class ).value()[0].converter());
	}

	@Test
	public void testEntityListeners() throws Exception {
		XMLContext context = buildContext( "org/hibernate/test/annotations/reflection/orm.xml" );

		Method method = Administration.class.getDeclaredMethod( "calculate" );
		JPAOverriddenAnnotationReader reader = new JPAOverriddenAnnotationReader( method, context, BootstrapContextImpl.INSTANCE );
		assertTrue( reader.isAnnotationPresent( PrePersist.class ) );

		reader = new JPAOverriddenAnnotationReader( Administration.class, context, BootstrapContextImpl.INSTANCE );
		assertTrue( reader.isAnnotationPresent( EntityListeners.class ) );
		assertEquals( 1, reader.getAnnotation( EntityListeners.class ).value().length );
		assertEquals( LogListener.class, reader.getAnnotation( EntityListeners.class ).value()[0] );

		method = LogListener.class.getDeclaredMethod( "noLog", Object.class );
		reader = new JPAOverriddenAnnotationReader( method, context, BootstrapContextImpl.INSTANCE );
		assertTrue( reader.isAnnotationPresent( PostLoad.class ) );

		method = LogListener.class.getDeclaredMethod( "log", Object.class );
		reader = new JPAOverriddenAnnotationReader( method, context, BootstrapContextImpl.INSTANCE );
		assertTrue( reader.isAnnotationPresent( PrePersist.class ) );
		assertFalse( reader.isAnnotationPresent( PostPersist.class ) );

		assertEquals( 1, context.getDefaultEntityListeners().size() );
		assertEquals( OtherLogListener.class.getName(), context.getDefaultEntityListeners().get( 0 ) );
	}

	private XMLContext buildContext(String ormfile) throws SAXException, DocumentException, IOException {
		XMLHelper xmlHelper = new XMLHelper();
		InputStream is = ClassLoaderServiceTestingImpl.INSTANCE.locateResourceStream( ormfile );
		assertNotNull( "ORM.xml not found: " + ormfile, is );
		XMLContext context = new XMLContext( BootstrapContextImpl.INSTANCE );
		ErrorLogger errorLogger = new ErrorLogger();
		SAXReader saxReader = xmlHelper.createSAXReader( errorLogger, EJB3DTDEntityResolver.INSTANCE );
		//saxReader.setValidation( false );
		try {
			saxReader.setFeature( "http://apache.org/xml/features/validation/schema", true );
		}
		catch ( SAXNotSupportedException e ) {
			saxReader.setValidation( false );
		}
		org.dom4j.Document doc;
		try {
			doc = saxReader.read( new InputSource( new BufferedInputStream( is ) ) );
		}
		finally {
			is.close();
		}
		if ( errorLogger.hasErrors() ) {
			System.out.println( errorLogger.getErrors().get( 0 ) );
		}
		assertFalse( errorLogger.hasErrors() );
		context.addDocument( doc );
		return context;
	}
}
