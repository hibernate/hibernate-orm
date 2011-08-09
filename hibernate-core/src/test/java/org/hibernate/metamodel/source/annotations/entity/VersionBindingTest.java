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

package org.hibernate.metamodel.source.annotations.entity;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import org.junit.Test;

import org.hibernate.annotations.Source;
import org.hibernate.annotations.SourceType;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.type.DbTimestampType;
import org.hibernate.type.TimestampType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Strong Liu
 */
public class VersionBindingTest extends BaseAnnotationBindingTestCase {
    @Entity
    class Item {
        @Id
        Long id;
        @Version
        Long version;
    }

    @Test
    @Resources(annotatedClasses = Item.class)
    public void testVersionAttributeType() {
        EntityBinding entityBinding = getEntityBinding( Item.class );
        AttributeBinding attributeBinding = entityBinding.locateAttributeBinding( "version" );
        assertTrue( entityBinding.isVersioned() );
        HibernateTypeDescriptor typeDescriptor = attributeBinding.getHibernateTypeDescriptor();
        assertNotNull( typeDescriptor );
        assertEquals( Long.class.getName(), typeDescriptor.getJavaTypeName() );
        assertTrue( typeDescriptor.getTypeParameters().isEmpty() );
    }

    @Entity
    class Item2 {
        @Id
        Long id;
        @Version
        Date version;
    }

    @Test
    @Resources(annotatedClasses = Item2.class)
    public void testVersionAttributeDefaultTemporalType() {
        EntityBinding entityBinding = getEntityBinding( Item2.class );
        AttributeBinding attributeBinding = entityBinding.locateAttributeBinding( "version" );
        assertTrue( entityBinding.isVersioned() );
        HibernateTypeDescriptor typeDescriptor = attributeBinding.getHibernateTypeDescriptor();
        assertNotNull( typeDescriptor );
        assertEquals( Date.class.getName(), typeDescriptor.getJavaTypeName() );
        assertTrue( typeDescriptor.getTypeParameters().isEmpty() );
        assertEquals( TimestampType.INSTANCE.getName(), typeDescriptor.getResolvedTypeMapping().getName() );
    }

        @Entity
    class Item3 {
        @Id
        Long id;
        @Version
                @Source(SourceType.DB)
        Date version;
    }

    @Test
    @Resources(annotatedClasses = Item3.class)
    public void testVersionAttributeWithSource() {
        EntityBinding entityBinding = getEntityBinding( Item3.class );
        AttributeBinding attributeBinding = entityBinding.locateAttributeBinding( "version" );
        assertTrue( entityBinding.isVersioned() );
        HibernateTypeDescriptor typeDescriptor = attributeBinding.getHibernateTypeDescriptor();
        assertNotNull( typeDescriptor );
        assertEquals( Date.class.getName(), typeDescriptor.getJavaTypeName() );
        assertTrue( typeDescriptor.getTypeParameters().isEmpty() );
        assertEquals( DbTimestampType.INSTANCE.getName(), typeDescriptor.getResolvedTypeMapping().getName() );
    }
}
