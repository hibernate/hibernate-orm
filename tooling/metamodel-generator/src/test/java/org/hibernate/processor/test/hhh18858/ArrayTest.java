/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.processor.test.hhh18858;


import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import static org.hibernate.processor.test.util.TestUtil.getFieldFromMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class ArrayTest extends CompilationTest {

	@Test
	@WithClasses({Competitor.class, Contest.class})
	public void testOneToMany() throws NoSuchFieldException, IllegalAccessException {
		System.out.println( getMetaModelSourceAsString( Competitor.class ) );
		assertValidMetamodelField( Competitor.class, "id" );
		assertValidMetamodelField( Competitor.class, "name" );

		System.out.println( getMetaModelSourceAsString( Contest.class ) );
		assertValidMetamodelField( Contest.class, "id" );
		assertValidMetamodelField( Contest.class, "results" );
		assertValidMetamodelField( Contest.class, "heldIn" );
	}

	private void assertValidMetamodelField(Class<?> entityClass, String fieldName)
			throws NoSuchFieldException, IllegalAccessException {
		final Field entityField = entityClass.getDeclaredField( fieldName );
		final Class<?> entityFieldType = entityField.getType();

		final Field modelField = getFieldFromMetamodelFor( entityClass, fieldName );
		final Type modelFieldGenericType = modelField.getGenericType();
		if (modelFieldGenericType instanceof ParameterizedType) {
            ParameterizedType parametrized = (ParameterizedType) modelFieldGenericType;
            final Type[] typeArguments = parametrized.getActualTypeArguments();
			assertEquals( 2, typeArguments.length );
			assertEquals( entityClass, typeArguments[0] );
			if ( Collection.class.isAssignableFrom( entityFieldType ) || entityFieldType.isArray() ) {
				assertEquals( ListAttribute.class, parametrized.getRawType() );
				if ( Collection.class.isAssignableFrom( entityFieldType ) ) {
					final ParameterizedType entityFieldGenericType = (ParameterizedType) entityField.getGenericType();
					assertEquals( entityFieldGenericType.getActualTypeArguments()[0], typeArguments[1] );
				}
				else if ( entityFieldType.getComponentType().isPrimitive() ) {
					assertEquals(
							entityFieldType.getComponentType(),
							((Class) typeArguments[1]).getDeclaredField( "TYPE" ).get( null )
					);
				}
				else {
					assertEquals( entityFieldType.getComponentType(), typeArguments[1] );
				}
			}
			else {
				assertEquals( SingularAttribute.class, parametrized.getRawType() );
				if ( entityFieldType.isPrimitive() ) {
					assertEquals(
							entityFieldType,
							((Class) typeArguments[1]).getDeclaredField( "TYPE" ).get( null )
					);
				}
				else {
					assertEquals( entityFieldType, typeArguments[1] );
				}
			}
		}
		else {
			Assertions.fail();
		}

	}

}
