/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.metamodel.internal.source.annotations.entity;

import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.type.BlobType;
import org.hibernate.type.CharacterArrayClobType;
import org.hibernate.type.ClobType;
import org.hibernate.type.MaterializedBlobType;
import org.hibernate.type.MaterializedClobType;
import org.hibernate.type.PrimitiveCharacterArrayClobType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.WrappedMaterializedBlobType;

import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Strong Liu
 */
public class LobBindingTests extends BaseAnnotationBindingTestCase {
    @Entity
    class Item {
        @Id
        long id;
        @Lob
        Clob clob;
        @Lob
        Blob blob;
        @Lob
        String str;
        @Lob
        Character[] characters;
        @Lob
        char[] chars;
        @Lob
        Byte[] bytes;
        @Lob
        byte[] bytes2;
        @Lob
        Thing serializable;
        String noLob;
    }

    class Thing implements Serializable {
        int size;
    }

    private HibernateTypeDescriptor getTypeDescriptor(String attributeName) {
        EntityBinding binding = getEntityBinding( Item.class );
        AttributeBinding attributeBinding = binding.locateAttributeBinding( attributeName );
        return attributeBinding.getHibernateTypeDescriptor();
    }

    private class ExpectedValue {
        String explicitTypeName;
        String javaTypeName;
        boolean isResolvedTypeMappingNull;
        Class resolvedTypeMappingClass;
        boolean isTypeParametersNull;
        boolean isTypeParametersEmpty;

        private ExpectedValue(String explicitTypeName,
                              String javaTypeName,
                              boolean resolvedTypeMappingNull,
                              Class resolvedTypeMappingClass,
                              boolean typeParametersNull,
                              boolean typeParametersEmpty
        ) {
            this.explicitTypeName = explicitTypeName;
            this.isResolvedTypeMappingNull = resolvedTypeMappingNull;
            this.isTypeParametersEmpty = typeParametersEmpty;
            this.isTypeParametersNull = typeParametersNull;
            this.javaTypeName = javaTypeName;
            this.resolvedTypeMappingClass = resolvedTypeMappingClass;
        }
    }

    private void checkHibernateTypeDescriptor(ExpectedValue expectedValue, String attributeName) {
        HibernateTypeDescriptor descriptor = getTypeDescriptor( attributeName );
        assertEquals( expectedValue.explicitTypeName, descriptor.getExplicitTypeName() );
        assertEquals( expectedValue.javaTypeName, descriptor.getJavaTypeDescriptor().getName().toString() );
        assertEquals( expectedValue.isResolvedTypeMappingNull, descriptor.getResolvedTypeMapping() == null );
        assertEquals( expectedValue.resolvedTypeMappingClass, descriptor.getResolvedTypeMapping().getClass() );
        assertEquals( expectedValue.isTypeParametersNull, descriptor.getTypeParameters() == null );
        assertEquals( expectedValue.isTypeParametersEmpty, descriptor.getTypeParameters().isEmpty() );
    }

    @Test
    @Resources(annotatedClasses = Item.class)
    public void testClobWithLobAnnotation() {
        ExpectedValue expectedValue = new ExpectedValue(
                "clob",
                Clob.class.getName(),
                false,
                ClobType.class,
                false,
                true
        );
        checkHibernateTypeDescriptor( expectedValue, "clob" );
    }

    @Test
    @Resources(annotatedClasses = Item.class)
    public void testBlobWithLobAnnotation() {
        ExpectedValue expectedValue = new ExpectedValue(
                "blob",
                Blob.class.getName(),
                false,
                BlobType.class,
                false,
                true
        );
        checkHibernateTypeDescriptor( expectedValue, "blob" );
    }

    @Test
    @Resources(annotatedClasses = Item.class)
    public void testStringWithLobAnnotation() {
        ExpectedValue expectedValue = new ExpectedValue(
                "materialized_clob",
                String.class.getName(),
                false,
                MaterializedClobType.class,
                false,
                true
        );
        checkHibernateTypeDescriptor( expectedValue, "str" );
    }

    @Test
    @Resources(annotatedClasses = Item.class)
    public void testCharacterArrayWithLobAnnotation() {
        ExpectedValue expectedValue = new ExpectedValue(
                CharacterArrayClobType.class.getName(),
                Character[].class.getName(),
                false,
                CharacterArrayClobType.class,
                false,
                true
        );
        checkHibernateTypeDescriptor( expectedValue, "characters" );
    }

    @Test
    @Resources(annotatedClasses = Item.class)
    public void testPrimitiveCharacterArrayWithLobAnnotation() {
        ExpectedValue expectedValue = new ExpectedValue(
                PrimitiveCharacterArrayClobType.class.getName(),
                char[].class.getName(),
                false,
                PrimitiveCharacterArrayClobType.class,
                false,
                true
        );
        checkHibernateTypeDescriptor( expectedValue, "chars" );
    }

    @Test
    @Resources(annotatedClasses = Item.class)
    public void testByteArrayWithLobAnnotation() {
        ExpectedValue expectedValue = new ExpectedValue(
                WrappedMaterializedBlobType.class.getName(),
                Byte[].class.getName(),
                false,
                WrappedMaterializedBlobType.class,
                false,
                true
        );
        checkHibernateTypeDescriptor( expectedValue, "bytes" );
    }

    @Test
    @Resources(annotatedClasses = Item.class)
    public void testPrimitiveByteArrayWithLobAnnotation() {
        ExpectedValue expectedValue = new ExpectedValue(
                StandardBasicTypes.MATERIALIZED_BLOB.getName(),
                byte[].class.getName(),
                false,
                MaterializedBlobType.class,
                false,
                true
        );
        checkHibernateTypeDescriptor( expectedValue, "bytes2" );
    }

    @Test
    @Resources(annotatedClasses = Item.class)
    public void testSerializableWithLobAnnotation() {
        ExpectedValue expectedValue = new ExpectedValue(
                SerializableToBlobType.class.getName(),
                Thing.class.getName(),
                false,
                SerializableToBlobType.class,
                false,
                false
        );
        checkHibernateTypeDescriptor( expectedValue, "serializable" );

        assertTrue(
                getTypeDescriptor( "serializable" ).getTypeParameters()
                        .get( SerializableToBlobType.CLASS_NAME )
                        .equals( Thing.class.getName() )
        );
    }


    @Test
    @Resources(annotatedClasses = Item.class)
    public void testNoLobAttribute() {
        assertNull( getTypeDescriptor( "noLob" ).getExplicitTypeName() );
        assertTrue( getTypeDescriptor( "noLob" ).getTypeParameters().isEmpty() );

    }
}
