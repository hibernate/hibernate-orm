package org.hibernate.envers.test.integration.onetoone.unidirectional;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Andrei Zagorneanu
 */
public class UnidirectionalCompositePKWithNulls extends AbstractEntityTest {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(AddressCompositePKEntity.class);
        cfg.addAnnotatedClass(PersonCompositeFKEntity.class);
    }

    @Test
    @Priority(10)
    public void initData() {
    	// address
        AddressCompositePKEntity address1 = new AddressCompositePKEntity(1, 1, "street1");

        // person with an address
        PersonCompositeFKEntity person1 = new PersonCompositeFKEntity(1, "person1", address1);
        // person without an address
        PersonCompositeFKEntity person2 = new PersonCompositeFKEntity(2, "person2", null);

        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        em.persist(person1);
        em.persist(person2);

        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1).equals(getAuditReader().getRevisions(PersonCompositeFKEntity.class, 1));
        assert Arrays.asList(1).equals(getAuditReader().getRevisions(PersonCompositeFKEntity.class, 2));
        assert Arrays.asList(1).equals(getAuditReader().getRevisions(AddressCompositePKEntity.class, new AddressPK(1, 1)));
    }

    @Test
    public void testHistoryOfPerson1() {
    	AddressCompositePKEntity address1 = getEntityManager().find(AddressCompositePKEntity.class, new AddressPK(1, 1));
        PersonCompositeFKEntity revPerson1 = getAuditReader().find(PersonCompositeFKEntity.class, 1, 1);
        assert address1 != null;
        assert revPerson1.getAddress().equals(address1);
    }

    @Test
    public void testHistoryOfPerson2NotNull() {
        PersonCompositeFKEntity revPerson2 = getAuditReader().find(PersonCompositeFKEntity.class, 2, 1);
        assert revPerson2.getAddress() == null;
    }
}