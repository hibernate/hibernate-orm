package org.hibernate.annotations.common.test.reflection.java.generics;

import java.lang.reflect.Type;

import junit.framework.TestCase;

import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironment;
import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironmentFactory;
import org.hibernate.annotations.common.reflection.java.generics.TypeUtils;

public class TypeUtilsTest extends TestCase {

	TypeEnvironmentFactory env = new TypeEnvironmentFactory();
	TypeEnvironment dadContext = env.getEnvironment( Dad.class );
	TypeEnvironment sonContext = env.getEnvironment( Son.class );

	public void testAClassIsAlwaysFullyResolved() throws Exception {
		assertTrue( TypeUtils.isResolved( Dad.class ) );
	}

	private Type getPropertyFromDad(String propertyName) throws NoSuchMethodException {
		return Dad.class.getMethod( propertyName, new Class[0] ).getGenericReturnType();
	}

	public void testKnowsWhetherAParametricTypeIsFullyResolved() throws Exception {
		Type simpleType = getPropertyFromDad( "returnsGeneric" );
		assertFalse( TypeUtils.isResolved( dadContext.bind( simpleType ) ) );
		assertTrue( TypeUtils.isResolved( sonContext.bind( simpleType ) ) );
	}

	public void testKnowsWhetherAnArrayTypeIsFullyResolved() throws Exception {
		Type arrayType = getPropertyFromDad( "getArrayProperty" );
		assertFalse( TypeUtils.isResolved( dadContext.bind( arrayType ) ) );
		assertTrue( TypeUtils.isResolved( sonContext.bind( arrayType ) ) );
	}
	
	public void testKnowsWhetherATypeIsSimple() throws Exception {
		assertTrue( TypeUtils.isSimple( String.class ) );
		assertFalse( TypeUtils.isSimple( new String[1].getClass() ) );
		assertFalse( TypeUtils.isSimple( getPropertyFromDad( "getNongenericCollectionProperty" ) ) );
		assertFalse( TypeUtils.isSimple( getPropertyFromDad( "getGenericCollectionProperty" ) ) );
	}
}
