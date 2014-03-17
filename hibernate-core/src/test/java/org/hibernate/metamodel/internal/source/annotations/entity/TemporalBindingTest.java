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

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.type.TimestampType;

import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Strong Liu
 */
public class TemporalBindingTest extends BaseAnnotationBindingTestCase {
    
	@Entity
    class Item2 {
        @Id
        long id;
        @Temporal(TemporalType.TIMESTAMP)
        Date date;
    }

    @Test
    @Resources(annotatedClasses = TemporalBindingTest.Item2.class)
    public void testTemporalTypeAttribute() {
        EntityBinding binding = getEntityBinding( Item2.class );
        AttributeBinding attributeBinding = binding.locateAttributeBinding( "date" );
        HibernateTypeDescriptor descriptor = attributeBinding.getHibernateTypeDescriptor();
        assertEquals( "timestamp", descriptor.getExplicitTypeName() );
        assertEquals( Date.class.getName(), descriptor.getJavaTypeDescriptor().getName().toString() );
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
    @Resources(annotatedClasses = TemporalBindingTest.Item3.class)
    public void testTemporalTypeAsId() {
        EntityBinding binding = getEntityBinding( Item3.class );
        AttributeBinding attributeBinding = binding.locateAttributeBinding( "date" );
        HibernateTypeDescriptor descriptor = attributeBinding.getHibernateTypeDescriptor();
        assertEquals( "timestamp", descriptor.getExplicitTypeName() );
        assertEquals( Date.class.getName(), descriptor.getJavaTypeDescriptor().getName().toString() );
        assertNotNull( descriptor.getResolvedTypeMapping() );
        assertEquals( TimestampType.class, descriptor.getResolvedTypeMapping().getClass() );
        assertNotNull( descriptor.getTypeParameters() );
        assertTrue( descriptor.getTypeParameters().isEmpty() );
    }
}
