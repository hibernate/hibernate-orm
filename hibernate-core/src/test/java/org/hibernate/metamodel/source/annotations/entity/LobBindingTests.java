package org.hibernate.metamodel.source.annotations.entity;

import java.io.Serializable;
import java.sql.Blob;
import java.sql.Clob;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.junit.Test;

import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.type.BlobType;
import org.hibernate.type.CharacterArrayClobType;
import org.hibernate.type.ClobType;
import org.hibernate.type.MaterializedBlobType;
import org.hibernate.type.MaterializedClobType;
import org.hibernate.type.PrimitiveCharacterArrayClobType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.WrappedMaterializedBlobType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

    class Thing implements Serializable{
        int size;
    }

    @Test
    @Resources(annotatedClasses = Item.class)
    public void testLobTypeAttribute() {
        EntityBinding binding = getEntityBinding( Item.class );

        AttributeBinding attributeBinding = binding.locateAttributeBinding( "clob" );
        HibernateTypeDescriptor descriptor = attributeBinding.getHibernateTypeDescriptor();
        assertEquals( "clob", descriptor.getExplicitTypeName() );
        assertEquals( Clob.class.getName(), descriptor.getJavaTypeName() );
        assertNotNull( descriptor.getResolvedTypeMapping() );
        assertEquals( ClobType.class, descriptor.getResolvedTypeMapping().getClass() );
        assertNotNull( descriptor.getTypeParameters() );
        assertTrue( descriptor.getTypeParameters().isEmpty() );

        attributeBinding = binding.locateAttributeBinding( "blob" );
        descriptor = attributeBinding.getHibernateTypeDescriptor();
        assertEquals( "blob", descriptor.getExplicitTypeName() );
        assertEquals( Blob.class.getName(), descriptor.getJavaTypeName() );
        assertNotNull( descriptor.getResolvedTypeMapping() );
        assertEquals( BlobType.class, descriptor.getResolvedTypeMapping().getClass() );
        assertNotNull( descriptor.getTypeParameters() );
        assertTrue( descriptor.getTypeParameters().isEmpty() );

        attributeBinding = binding.locateAttributeBinding( "str" );
        descriptor = attributeBinding.getHibernateTypeDescriptor();
        assertEquals( "materialized_clob", descriptor.getExplicitTypeName() );
        assertEquals( String.class.getName(), descriptor.getJavaTypeName() );
        assertNotNull( descriptor.getResolvedTypeMapping() );
        assertEquals( MaterializedClobType.class, descriptor.getResolvedTypeMapping().getClass() );
        assertNotNull( descriptor.getTypeParameters() );
        assertTrue( descriptor.getTypeParameters().isEmpty() );

        attributeBinding = binding.locateAttributeBinding( "characters" );
        descriptor = attributeBinding.getHibernateTypeDescriptor();
        assertEquals( CharacterArrayClobType.class.getName(), descriptor.getExplicitTypeName() );
        assertEquals( Character[].class.getName(), descriptor.getJavaTypeName() );
        assertNotNull( descriptor.getResolvedTypeMapping() );
        assertEquals( CharacterArrayClobType.class, descriptor.getResolvedTypeMapping().getClass() );
        assertNotNull( descriptor.getTypeParameters() );
        assertTrue( descriptor.getTypeParameters().isEmpty() );

        attributeBinding = binding.locateAttributeBinding( "chars" );
        descriptor = attributeBinding.getHibernateTypeDescriptor();
        assertEquals( PrimitiveCharacterArrayClobType.class.getName(), descriptor.getExplicitTypeName() );
        assertEquals( char[].class.getName(), descriptor.getJavaTypeName() );
        assertNotNull( descriptor.getResolvedTypeMapping() );
        assertEquals( PrimitiveCharacterArrayClobType.class, descriptor.getResolvedTypeMapping().getClass() );
        assertNotNull( descriptor.getTypeParameters() );
        assertTrue( descriptor.getTypeParameters().isEmpty() );

        attributeBinding = binding.locateAttributeBinding( "bytes" );
        descriptor = attributeBinding.getHibernateTypeDescriptor();
        assertEquals( WrappedMaterializedBlobType.class.getName(), descriptor.getExplicitTypeName() );
        assertEquals( Byte[].class.getName(), descriptor.getJavaTypeName() );
        assertNotNull( descriptor.getResolvedTypeMapping() );
        assertEquals( WrappedMaterializedBlobType.class, descriptor.getResolvedTypeMapping().getClass() );
        assertNotNull( descriptor.getTypeParameters() );
        assertTrue( descriptor.getTypeParameters().isEmpty() );

        attributeBinding = binding.locateAttributeBinding( "bytes2" );
        descriptor = attributeBinding.getHibernateTypeDescriptor();
        assertEquals( StandardBasicTypes.MATERIALIZED_BLOB.getName(), descriptor.getExplicitTypeName() );
        assertEquals( byte[].class.getName(), descriptor.getJavaTypeName() );
        assertNotNull( descriptor.getResolvedTypeMapping() );
        assertEquals( MaterializedBlobType.class, descriptor.getResolvedTypeMapping().getClass() );
        assertNotNull( descriptor.getTypeParameters() );
        assertTrue( descriptor.getTypeParameters().isEmpty() );

        attributeBinding = binding.locateAttributeBinding( "serializable" );
        descriptor = attributeBinding.getHibernateTypeDescriptor();
        assertEquals( SerializableToBlobType.class.getName(), descriptor.getExplicitTypeName() );
        assertEquals( Thing.class.getName(), descriptor.getJavaTypeName() );
        assertNotNull( descriptor.getResolvedTypeMapping() );
        assertEquals( SerializableToBlobType.class, descriptor.getResolvedTypeMapping().getClass() );
        assertNotNull( descriptor.getTypeParameters() );
        assertEquals( 1,descriptor.getTypeParameters().size() );
        assertTrue( descriptor.getTypeParameters().get( SerializableToBlobType.CLASS_NAME ).equals( Thing.class.getName() ) );

        attributeBinding = binding.locateAttributeBinding( "noLob" );
        descriptor = attributeBinding.getHibernateTypeDescriptor();
        assertNull( descriptor.getExplicitTypeName() );
        assertTrue( descriptor.getTypeParameters().isEmpty() );

    }
}
