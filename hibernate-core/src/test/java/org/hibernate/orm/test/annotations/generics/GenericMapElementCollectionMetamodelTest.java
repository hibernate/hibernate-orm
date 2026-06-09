/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.MapAttribute;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel( annotatedClasses = {
		GenericMapElementCollectionMetamodelTest.IntegerValueOwner.class,
		GenericMapElementCollectionMetamodelTest.StringValueOwner.class,
} )
public class GenericMapElementCollectionMetamodelTest {
	@Test
	public void testMetamodelSpecializesMapValueType(SessionFactoryScope scope) {
		final EntityType<IntegerValueOwner> ownerType = scope.getSessionFactory().getMetamodel()
				.entity( IntegerValueOwner.class );
		final MapAttribute<? super IntegerValueOwner, String, Integer> values =
				ownerType.getMap( "values", String.class, Integer.class );

		assertThat( values.getJavaType() ).isEqualTo( Map.class );
		assertThat( values.getKeyJavaType() ).isEqualTo( String.class );
		assertThat( values.getElementType().getJavaType() ).isEqualTo( Integer.class );
	}

	@Test
	public void testMetamodelSpecializesMapValueTypePerConcreteOwner(SessionFactoryScope scope) {
		final EntityType<IntegerValueOwner> integerOwnerType = scope.getSessionFactory().getMetamodel()
				.entity( IntegerValueOwner.class );
		final EntityType<StringValueOwner> stringOwnerType = scope.getSessionFactory().getMetamodel()
				.entity( StringValueOwner.class );

		final MapAttribute<? super IntegerValueOwner, String, Integer> integerValues =
				integerOwnerType.getMap( "values", String.class, Integer.class );
		final MapAttribute<? super StringValueOwner, String, String> stringValues =
				stringOwnerType.getMap( "values", String.class, String.class );

		assertThat( integerValues.getKeyJavaType() ).isEqualTo( String.class );
		assertThat( integerValues.getElementType().getJavaType() ).isEqualTo( Integer.class );
		assertThat( stringValues.getKeyJavaType() ).isEqualTo( String.class );
		assertThat( stringValues.getElementType().getJavaType() ).isEqualTo( String.class );
	}

	@MappedSuperclass
	public abstract static class AbstractOwner<T> {
		@Id
		private Long id;

		@ElementCollection
		@MapKeyColumn( name = "value_key" )
		private Map<String, T> values = new HashMap<>();
	}

	@Entity( name = "IntegerValueOwner" )
	public static class IntegerValueOwner extends AbstractOwner<Integer> {
	}

	@Entity( name = "StringValueOwner" )
	public static class StringValueOwner extends AbstractOwner<String> {
	}
}
