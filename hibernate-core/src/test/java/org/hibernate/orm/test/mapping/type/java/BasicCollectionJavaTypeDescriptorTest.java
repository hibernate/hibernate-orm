/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.collection.internal.StandardSetSemantics;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.java.spi.BasicCollectionJavaType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * @author Yanming Zhou
 */
public class BasicCollectionJavaTypeDescriptorTest {

	private static final BasicCollectionJavaType<Set<String>, String> stringSetJavaType = createJavaType();

	@Test
	@JiraKey( "HHH-18073" )
	public void wrapShouldRetainOrderOfSet() {
		assertInstanceOf( LinkedHashSet.class, stringSetJavaType.wrap( Set.of( "foo", "bar" ), null ),
				"BasicCollectionJavaType.wrap() should retain order of Set" );
	}

	@Test
	public void deepCopyShouldRetainOrderOfSet() {
		assertInstanceOf( LinkedHashSet.class,
				stringSetJavaType.getMutabilityPlan().deepCopy( Set.of( "foo", "bar" ) ),
				"BasicCollectionJavaType.getMutabilityPlan().deepCopy() should retain order of Set" );
	}

	@SuppressWarnings("unchecked")
	private static BasicCollectionJavaType<Set<String>, String> createJavaType() {
		ParameterizedType type;
		try {
			type = (ParameterizedType) TestEntity.class.getDeclaredField("tags").getGenericType();
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
		return new BasicCollectionJavaType<>(
				type,
				StringJavaType.INSTANCE,
				(StandardSetSemantics<String>) StandardSetSemantics.INSTANCE
		);
	}

	@Entity
	static class TestEntity {

		@Id
		@GeneratedValue
		Long id;

		Set<String> tags;
	}
}
