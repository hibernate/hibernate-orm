// $Id: Dom4jAccessorTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.entitymode.dom4j.accessors;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.util.NodeComparator;

import org.hibernate.EntityMode;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Mappings;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;

/**
 * Unit test of dom4j-based accessors
 *
 * @author Steve Ebersole
 */
public class Dom4jAccessorTest extends TestCase {

	public static final Element DOM = generateTestElement();

	private Mappings mappings;

	public Dom4jAccessorTest(String name) {
		super( name );
	}

	@Override
	protected void setUp() throws Exception {
		mappings = new Configuration().createMappings();
	}

	public void testStringElementExtraction() throws Throwable {
		Property property = generateNameProperty();
		Getter getter = PropertyAccessorFactory.getPropertyAccessor( property, EntityMode.DOM4J )
				.getGetter( null, null );
		String name = ( String ) getter.get( DOM );
		assertEquals( "Not equals", "JBoss", name );
	}

	public void testStringTextExtraction() throws Throwable {
		Property property = generateTextProperty();
		Getter getter = PropertyAccessorFactory.getPropertyAccessor( property, EntityMode.DOM4J )
				.getGetter( null, null );
		String name = ( String ) getter.get( DOM );
		assertEquals( "Not equals", "description...", name );
	}

	public void testLongAttributeExtraction() throws Throwable {
		Property property = generateIdProperty();
		Getter getter = PropertyAccessorFactory.getPropertyAccessor( property, EntityMode.DOM4J )
				.getGetter( null, null );
		Long id = ( Long ) getter.get( DOM );
		assertEquals( "Not equals", new Long( 123 ), id );
	}

	public void testLongElementAttributeExtraction() throws Throwable {
		Property property = generateAccountIdProperty();
		Getter getter = PropertyAccessorFactory.getPropertyAccessor( property, EntityMode.DOM4J )
				.getGetter( null, null );
		Long id = ( Long ) getter.get( DOM );
		assertEquals( "Not equals", new Long( 456 ), id );
	}

	public void testCompanyElementGeneration() throws Throwable {
		Setter idSetter = PropertyAccessorFactory.getPropertyAccessor( generateIdProperty(), EntityMode.DOM4J )
				.getSetter( null, null );
		Setter nameSetter = PropertyAccessorFactory.getPropertyAccessor( generateNameProperty(), EntityMode.DOM4J )
				.getSetter( null, null );
		Setter textSetter = PropertyAccessorFactory.getPropertyAccessor( generateTextProperty(), EntityMode.DOM4J )
				.getSetter( null, null );
		Setter accountIdSetter = PropertyAccessorFactory.getPropertyAccessor(
				generateAccountIdProperty(), EntityMode.DOM4J
		)
				.getSetter( null, null );

		Element root = generateRootTestElement();

		idSetter.set( root, new Long( 123 ), getSFI() );
		textSetter.set( root, "description...", getSFI() );
		nameSetter.set( root, "JBoss", getSFI() );
		accountIdSetter.set( root, new Long( 456 ), getSFI() );

		assertTrue( "DOMs not equal", new NodeComparator().compare( DOM, root ) == 0 );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private static Element generateTestElement() {
		Element company = generateRootTestElement();
		company.addAttribute( "id", "123" );
		company.setText( "description..." );
		company.addElement( "name" ).setText( "JBoss" );
		company.addElement( "account" ).addAttribute( "num", "456" );

		return company;
	}

	private static Element generateRootTestElement() {
		return DocumentFactory.getInstance().createElement( "company" );
	}

	public static Test suite() {
		return new TestSuite( Dom4jAccessorTest.class );
	}

	private SessionFactoryImplementor getSFI() {
		return null;
	}

	private Property generateIdProperty() {
		SimpleValue value = new SimpleValue( mappings );
		value.setTypeName( "long" );

		Property property = new Property();
		property.setName( "id" );
		property.setNodeName( "@id" );
		property.setValue( value );

		return property;
	}

	private Property generateTextProperty() {
		SimpleValue value = new SimpleValue(mappings);
		value.setTypeName( "string" );

		Property property = new Property();
		property.setName( "text" );
		property.setNodeName( "." );
		property.setValue( value );

		return property;
	}

	private Property generateAccountIdProperty() {
		SimpleValue value = new SimpleValue(mappings);
		value.setTypeName( "long" );

		Property property = new Property();
		property.setName( "number" );
		property.setNodeName( "account/@num" );
		property.setValue( value );

		return property;
	}

	private Property generateNameProperty() {
		SimpleValue value = new SimpleValue(mappings);
		value.setTypeName( "string" );

		Property property = new Property();
		property.setName( "name" );
		property.setNodeName( "name" );
		property.setValue( value );

		return property;
	}
}
