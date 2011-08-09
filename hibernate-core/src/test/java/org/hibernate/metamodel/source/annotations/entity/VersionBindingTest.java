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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import org.junit.Test;
import static org.junit.Assert.*;

import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;

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
        HibernateTypeDescriptor typeDescriptor= attributeBinding.getHibernateTypeDescriptor();
        assertNotNull( typeDescriptor );
        assertTrue( typeDescriptor.getTypeParameters().isEmpty() );
    }
}
