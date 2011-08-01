package org.hibernate.metamodel.source.annotations.entity;

import java.sql.Types;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.junit.Test;

import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Strong Liu
 */
public class EnumeratedBindingTests extends BaseAnnotationBindingTestCase {
    @Entity
    class Item {
        @Id
        long id;
        @Temporal(TemporalType.TIMESTAMP)
        Date orderDate;
        String name;
        @Enumerated(EnumType.STRING)
        OrderType orderType;
        CustomerType customerType;
    }

    enum CustomerType {
        PROGRAMMER, BOSS;
    }

    enum OrderType {
        B2C, C2C, MAIL, DIRECT;
    }

    @Test
    @Resources(annotatedClasses = Item.class)
    public void testEnumeratedTypeAttribute() {
        EntityBinding binding = getEntityBinding( Item.class );

        AttributeBinding attributeBinding = binding.locateAttributeBinding( "customerType" );
        HibernateTypeDescriptor descriptor = attributeBinding.getHibernateTypeDescriptor();
        assertEquals( org.hibernate.type.EnumType.class.getName(), descriptor.getExplicitTypeName() );
        assertEquals( CustomerType.class.getName(), descriptor.getJavaTypeName() );
        assertNotNull( descriptor.getResolvedTypeMapping() );
        assertFalse( descriptor.getTypeParameters().isEmpty() );
        assertEquals(
                CustomerType.class.getName(),
                descriptor.getTypeParameters().get( org.hibernate.type.EnumType.ENUM )
        );
        assertEquals(
                String.valueOf( Types.INTEGER ),
                descriptor.getTypeParameters().get( org.hibernate.type.EnumType.TYPE )
        );


        attributeBinding = binding.locateAttributeBinding( "orderType" );
        descriptor = attributeBinding.getHibernateTypeDescriptor();
        assertEquals( org.hibernate.type.EnumType.class.getName(), descriptor.getExplicitTypeName() );
        assertEquals( OrderType.class.getName(), descriptor.getJavaTypeName() );
        assertNotNull( descriptor.getResolvedTypeMapping() );
        assertFalse( descriptor.getTypeParameters().isEmpty() );
        assertEquals(
                OrderType.class.getName(),
                descriptor.getTypeParameters().get( org.hibernate.type.EnumType.ENUM )
        );
        assertEquals(
                String.valueOf( Types.VARCHAR ),
                descriptor.getTypeParameters().get( org.hibernate.type.EnumType.TYPE )
        );
    }

}
