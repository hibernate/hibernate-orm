/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.java;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.collection.internal.StandardSetSemantics;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.java.spi.BasicCollectionJavaType;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * @author Yanming Zhou
 */
public class BasicCollectionJavaTypeDescriptorTest {

    private static final BasicCollectionJavaType<Set<String>, String> stringSetJavaType = createJavaType();

    @Test
    @JiraKey( "HHH-18073" )
    public void wrapShouldRetainOrderOfSet() {
        assertTrue("BasicCollectionJavaType.wrap() should retain order of Set",
                stringSetJavaType.wrap(Set.of( "foo", "bar" ), null) instanceof LinkedHashSet);
    }

    @Test
    public void deepCopyShouldRetainOrderOfSet() {
        assertTrue("BasicCollectionJavaType.getMutabilityPlan().deepCopy() should retain order of Set",
                stringSetJavaType.getMutabilityPlan().deepCopy(Set.of( "foo", "bar" )) instanceof LinkedHashSet);
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
