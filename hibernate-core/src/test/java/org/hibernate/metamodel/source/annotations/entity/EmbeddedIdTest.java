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

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.junit.Test;

import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityIdentifier;
import org.hibernate.testing.FailureExpected;

import static junit.framework.Assert.assertTrue;

/**
 * @author Strong Liu
 */
@FailureExpected(jiraKey = "HHH-6447", message = "Work in progress")
public class EmbeddedIdTest extends BaseAnnotationBindingTestCase {
    @Test
//	@Resources(annotatedClasses = { User.class, Address.class })
    public void testEmbeddable() {
        EntityBinding binding = getEntityBinding( User.class );
        EntityIdentifier identifier = binding.getHierarchyDetails().getEntityIdentifier();
        assertTrue( identifier.isEmbedded() );
    }

    @Entity
    @Access( AccessType.FIELD )
    class User {
        private String name;
        @EmbeddedId
        private Address address;
    }

    @Embeddable
    class Address {
        String street;
        String city;
        String postCode;
    }
}



