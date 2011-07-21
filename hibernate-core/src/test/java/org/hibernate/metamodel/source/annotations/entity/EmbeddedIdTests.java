package org.hibernate.metamodel.source.annotations.entity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.junit.Test;

import org.hibernate.id.Assigned;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityIdentifier;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.Component;
import org.hibernate.testing.FailureExpected;

import static junit.framework.Assert.assertTrue;

/**
 * @author Strong Liu
 */
@FailureExpected(jiraKey = "HHH-6447", message = "Work in progress")
public class EmbeddedIdTests extends BaseAnnotationBindingTestCase {
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



