package org.hibernate.annotations.common.test.reflection.java.generics;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironmentFactory;

/**
 * @author Davide Marchignoli
 * @author Paolo Perrotta
 */
public class TypeEnvironmentFactoryTest extends TestCase {

	public void testBindsGenericsToSuperclassEnvironment() throws SecurityException, NoSuchMethodException {
		TypeEnvironmentFactory env = new TypeEnvironmentFactory();
		Type type = Grandpa.class.getMethod( "returnsGeneric", new Class[0] ).getGenericReturnType();

		Type asSeenFromGrandpa = env.getEnvironment( Grandpa.class ).bind( type );
		assertTrue( asSeenFromGrandpa instanceof TypeVariable );
		assertEquals( "T", asSeenFromGrandpa.toString() );

		Type asSeenFromDad = env.getEnvironment( Dad.class ).bind( type );
		assertTrue( asSeenFromDad instanceof ParameterizedType );
		assertEquals( "java.util.List<T>", asSeenFromDad.toString() );

		ParameterizedType asSeenFromSon = (ParameterizedType) env.getEnvironment( Son.class ).bind( type );
		assertType_isCollectionOfClass_withElementsOfClass( asSeenFromSon, List.class, String.class );
	}

	public void testBindsGenericsToOwnerEnvironment() throws SecurityException, NoSuchMethodException {
		TypeEnvironmentFactory env = new TypeEnvironmentFactory();

		Type friendType = Dad.class.getMethod( "getFriend", new Class[0] ).getGenericReturnType();
		ParameterizedType friendTypeAsSeenFromDad = (ParameterizedType) env.getEnvironment( Dad.class ).bind(
				friendType
		);

		Class friendClass = (Class) friendTypeAsSeenFromDad.getRawType();
		Type returnType = friendClass.getMethod( "embeddedProperty", new Class[0] ).getGenericReturnType();

		ParameterizedType boundType = (ParameterizedType) env.getEnvironment( friendTypeAsSeenFromDad ).bind(
				returnType
		);
		assertType_isCollectionOfClass_withElementsOfClass( boundType, Set.class, Integer.class );
	}

	private void assertType_isCollectionOfClass_withElementsOfClass(
			ParameterizedType t, Class collectionClass,
			Class elementClass
	) {
		assertEquals( collectionClass, t.getRawType() );
		assertEquals( 1, t.getActualTypeArguments().length );
		assertEquals( elementClass, t.getActualTypeArguments()[0] );
	}
}
