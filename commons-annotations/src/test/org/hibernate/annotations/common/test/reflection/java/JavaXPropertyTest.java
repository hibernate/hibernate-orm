package org.hibernate.annotations.common.test.reflection.java;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.AssertionFailedError;

import org.hibernate.annotations.common.reflection.Filter;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.annotations.common.test.reflection.java.generics.Dad;
import org.hibernate.annotations.common.test.reflection.java.generics.Son;

/**
 * @author Paolo Perrotta
 */
public class JavaXPropertyTest extends XAnnotatedElementTestCase {

	private ReflectionManager factory = new JavaReflectionManager();

	private XClass dadAsSeenFromItself = factory.toXClass( Dad.class );

	private XClass dadAsSeenFromSon = factory.toXClass( Son.class ).getSuperclass();

	public void testFollowsJavaBeansConventionsForPropertyNames() throws Exception {
		List<String> properties = new LinkedList<String>();
		properties.add( "collectionProperty" );
		properties.add( "methodProperty" );
		properties.add( "primitiveProperty" );
		properties.add( "primitiveArrayProperty" );
		properties.add( "arrayProperty" );
		properties.add( "genericCollectionProperty" );
		properties.add( "nongenericCollectionProperty" );
		properties.add( "propertyStartingWithIs" );
		properties.add( "language" );
		List<XProperty> methodProperties = dadAsSeenFromSon.getDeclaredProperties( "property" );
		assertEquals( properties.size(), methodProperties.size() );
		for ( XProperty member : methodProperties ) {
			assertTrue( properties.contains( member.getName() ) );
		}
		List<XProperty> fieldProperties = dadAsSeenFromSon.getDeclaredProperties( "field" );
		XProperty field = fieldProperties.get( 0 );
		assertEquals( "fieldProperty", field.getName() );
	}

	public void testReturnsPropertiesWithUnresolvedParametricTypes() {
		assertEquals( 9, dadAsSeenFromItself.getDeclaredProperties( "property" ).size() );
	}

	public void testKnowsWhetherItsTypeIsFullyResolved() {
		XProperty notFullyResolvedProperty = getPropertyNamed_from(
				"collectionProperty", dadAsSeenFromItself
				.getDeclaredProperties( "property" )
		);
		assertFalse( notFullyResolvedProperty.isTypeResolved() );
		XProperty fullyResolvedProperty = getPropertyNamed_from(
				"collectionProperty", dadAsSeenFromSon
				.getDeclaredProperties( "property" )
		);
		assertTrue( fullyResolvedProperty.isTypeResolved() );
	}

	public void testCanBeFiltered() {
		assertEquals(
				10, dadAsSeenFromSon.getDeclaredProperties(
				"property", new Filter() {

			public boolean returnStatic() {
				return true;
			}

			public boolean returnTransient() {
				return false;
			}
		}
		).size()
		);
	}

	public void testCanBeASimpleType() {
		List<XProperty> declaredProperties = dadAsSeenFromSon.getDeclaredProperties( "field" );
		XProperty p = getPropertyNamed_from( "fieldProperty", declaredProperties );
		assertTrue( factory.equals( p.getType(), String.class ) );
		assertTrue( factory.equals( p.getElementClass(), String.class ) );
		assertTrue( factory.equals( p.getClassOrElementClass(), String.class ) );
		assertNull( p.getCollectionClass() );
		assertFalse( p.isArray() );
		assertFalse( p.isCollection() );
	}

	public void testResolveInterfaceType() {
		List<XProperty> declaredProperties = dadAsSeenFromSon.getDeclaredProperties( "property" );
		XProperty p = getPropertyNamed_from( "language", declaredProperties );
		assertTrue( factory.equals( p.getType(), String.class ) );
		assertTrue( factory.equals( p.getElementClass(), String.class ) );
		assertTrue( factory.equals( p.getClassOrElementClass(), String.class ) );
		assertNull( p.getCollectionClass() );
		assertNull( p.getMapKey() );
		assertFalse( p.isArray() );
		assertFalse( p.isCollection() );
	}

	public void testCanBeAnArray() {
		List<XProperty> declaredProperties = dadAsSeenFromSon.getDeclaredProperties( "property" );
		XProperty p = getPropertyNamed_from( "arrayProperty", declaredProperties );
		assertTrue( factory.equals( p.getType(), String[].class ) );
		assertTrue( factory.equals( p.getElementClass(), String.class ) );
		assertTrue( factory.equals( p.getClassOrElementClass(), String.class ) );
		assertNull( p.getCollectionClass() );
		assertNull( p.getMapKey() );
		assertTrue( p.isArray() );
		assertFalse( p.isCollection() );
	}

	public void testCanBeAnArrayOfPrimitives() {
		List<XProperty> declaredProperties = dadAsSeenFromSon.getDeclaredProperties( "property" );
		XProperty p = getPropertyNamed_from( "primitiveArrayProperty", declaredProperties );
		assertTrue( factory.equals( p.getType(), int[].class ) );
		assertTrue( factory.equals( p.getElementClass(), int.class ) );
		assertTrue( factory.equals( p.getClassOrElementClass(), int.class ) );
		assertNull( p.getCollectionClass() );
		assertNull( p.getMapKey() );
		assertTrue( p.isArray() );
		assertFalse( p.isCollection() );
	}

	public void testCanBeACollection() {
		List<XProperty> declaredProperties = dadAsSeenFromSon.getDeclaredProperties( "property" );
		XProperty p = getPropertyNamed_from( "collectionProperty", declaredProperties );
		assertTrue( factory.equals( p.getType(), Map.class ) );
		assertTrue( factory.equals( p.getElementClass(), String.class ) );
		assertTrue( factory.equals( p.getClassOrElementClass(), Map.class ) );
		assertTrue( factory.equals( p.getMapKey(), Double.class ) );
		assertEquals( Map.class, p.getCollectionClass() );
		assertFalse( p.isArray() );
		assertTrue( p.isCollection() );
	}

	private XProperty getPropertyNamed_from(String name, List<XProperty> properties) {
		for ( XProperty p : properties ) {
			if ( p.getName().equals( name ) ) {
				return p;
			}
		}
		throw new AssertionFailedError( "No property '" + name + "' found" );
	}

	public void testSupportsMethodsStartingWithIs() throws Exception {
		assertEquals( "methodProperty", getConcreteInstance().getName() );
	}

	@Override
	protected XProperty getConcreteInstance() {
		XClass xClass = factory.toXClass( Dad.class );
		List<XProperty> properties = xClass.getDeclaredProperties( "property" );
		for ( XProperty p : properties ) {
			if ( p.getName().equals( "methodProperty" ) ) {
				return p;
			}
		}
		throw new AssertionFailedError( "Cannot find Foo.getMethodProperty()" );
	}
}
