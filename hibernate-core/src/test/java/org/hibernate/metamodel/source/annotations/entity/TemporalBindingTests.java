package org.hibernate.metamodel.source.annotations.entity;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.junit.Test;

import org.hibernate.AnnotationException;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.type.TimestampType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Strong Liu
 */
public class TemporalBindingTests extends BaseAnnotationBindingTestCase {
    @Entity
    class Item1 {
        @Id
        long id;
        Date date;
    }

    @Test(expected = AnnotationException.class)
    @Resources(annotatedClasses = TemporalBindingTests.Item1.class)
    public void testNoTemporalAnnotationOnTemporalTypeAttribute() {
        getEntityBinding( Item1.class );

    }

    @Entity
    class Item2 {
        @Id
        long id;
        @Temporal(TemporalType.TIMESTAMP)
        Date date;
    }

    @Test
    @Resources(annotatedClasses = TemporalBindingTests.Item2.class)
    public void testTemporalTypeAttribute() {
        EntityBinding binding = getEntityBinding( Item2.class );
        AttributeBinding attributeBinding = binding.getAttributeBinding( "date" );
        HibernateTypeDescriptor descriptor = attributeBinding.getHibernateTypeDescriptor();
        assertEquals( "timestamp", descriptor.getExplicitTypeName() );
        assertEquals( Date.class.getName(), descriptor.getJavaTypeName() );
        assertNotNull( descriptor.getResolvedTypeMapping() );
        assertEquals( TimestampType.class, descriptor.getResolvedTypeMapping().getClass() );
        assertNotNull( descriptor.getTypeParameters() );
        assertTrue( descriptor.getTypeParameters().isEmpty() );
    }

    @Entity
    class Item3 {
        @Id
        @Temporal(TemporalType.TIMESTAMP)
        Date date;
    }

    @Test
    @Resources(annotatedClasses = TemporalBindingTests.Item3.class)
    public void testTemporalTypeAsId() {
        EntityBinding binding = getEntityBinding( Item3.class );
        AttributeBinding attributeBinding = binding.getAttributeBinding( "date" );
        HibernateTypeDescriptor descriptor = attributeBinding.getHibernateTypeDescriptor();
        assertEquals( "timestamp", descriptor.getExplicitTypeName() );
        assertEquals( Date.class.getName(), descriptor.getJavaTypeName() );
        assertNotNull( descriptor.getResolvedTypeMapping() );
        assertEquals( TimestampType.class, descriptor.getResolvedTypeMapping().getClass() );
        assertNotNull( descriptor.getTypeParameters() );
        assertTrue( descriptor.getTypeParameters().isEmpty() );
    }
}
