package org.hibernate.annotations.common.test.reflection.java.generics;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import junit.framework.TestCase;
import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironment;
import org.hibernate.annotations.common.reflection.java.generics.TypeEnvironmentFactory;

public class ApproximatingTypeEnvironmentTest extends TestCase {

	TypeEnvironmentFactory teFactory = new TypeEnvironmentFactory();
	TypeEnvironment unboundContext = teFactory.getEnvironment( BigBlob.class );
	TypeEnvironment approximatingUnboundContext = teFactory.toApproximatingEnvironment( unboundContext );

	TypeEnvironment boundContext = teFactory.getEnvironment( SonOfBlob.class );
	TypeEnvironment approximatingBoundContext = teFactory.toApproximatingEnvironment( boundContext );

	public void testDoesNothingOnClasses() throws SecurityException {
		assertEquals( String[].class, approximatingUnboundContext.bind( String[].class ) );
	}

	public void testDoesNothingOnWildcards() throws Exception {
		Type type = BigBlob.class.getMethod( "genericClass", new Class[0] ).getGenericReturnType();
		Type approxType = approximatingBoundContext.bind( type );
		assertEquals( "java.lang.Class<?>", approxType.toString() );
	}

	public void testDoesNothingOnParameterizedTypesThatAreAlreadyFullyBound() throws Exception {
		Type type = BigBlob.class.getMethod( "simpleGenericType", new Class[0] ).getGenericReturnType();
		assertEquals( boundContext.bind( type ), approximatingBoundContext.bind( type ) );
	}

	public void testDoesNothingOnComplexParameterizedTypesThatAreNotCollections() throws Exception {
		Type type = BigBlob.class.getMethod( "genericType", new Class[0] ).getGenericReturnType();
		assertEquals( boundContext.bind( type ), approximatingBoundContext.bind( type ) );
	}

	public void testDoesNothingOnGenericArraysThatAreAlreadyFullyBound() throws Exception {
		Type type = BigBlob.class.getMethod( "array", new Class[0] ).getGenericReturnType();
		assertEquals( boundContext.bind( type ), approximatingBoundContext.bind( type ) );
	}

	public void testApproximatesSimpleGenericTypesToTheirUpperBound() throws Exception {
		Type type = BigBlob.class.getMethod( "simpleGenericType", new Class[0] ).getGenericReturnType();
		assertEquals( "java.util.List<java.lang.String>", approximatingBoundContext.bind( type ).toString() );
	}

	public void testApproximatesGenericsInArraysToTheirUpperBounds() throws Exception {
		Type type = BigBlob.class.getMethod( "array", new Class[0] ).getGenericReturnType();
		assertEquals( Collection[].class, approximatingUnboundContext.bind( type ) );
	}

	public void testApproximatesArraysOfComplexTypesToArraysOfObjects() throws Exception {
		Type type = BigBlob.class.getMethod( "complexGenericArray", new Class[0] ).getGenericReturnType();
		assertEquals( Object[].class, approximatingUnboundContext.bind( type ) );
	}

	public void testApproximatesGenericsAndWildcardsInCollectionsToTheirUpperBounds() throws Exception {
		Type type = BigBlob.class.getMethod( "genericCollection", new Class[0] ).getGenericReturnType();
		ParameterizedType approxType = (ParameterizedType) approximatingUnboundContext.bind( type );
		assertEquals( Map.class, approxType.getRawType() );
		assertNull( approxType.getOwnerType() );
		assertEquals( 2, approxType.getActualTypeArguments().length );
		assertEquals( Object.class, approxType.getActualTypeArguments()[0] );
		assertEquals( Collection.class, approxType.getActualTypeArguments()[1] );
	}
}
