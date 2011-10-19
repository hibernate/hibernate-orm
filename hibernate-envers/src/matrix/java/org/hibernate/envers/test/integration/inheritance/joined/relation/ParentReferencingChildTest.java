package org.hibernate.envers.test.integration.inheritance.joined.relation;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-3843")
public class ParentReferencingChildTest extends AbstractEntityTest {
    Person expLukaszRev1 = null;
    Person expLukaszRev2 = null;
    Person expAdamRev4 = null;
    Role expDirectorRev3 = null;
    Role expAdminRev2 = null;
    Role expAdminRev1 = null;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(Person.class);
        cfg.addAnnotatedClass(Role.class);
        cfg.addAnnotatedClass(RightsSubject.class);
    }

    @Test
    @Priority(10)
    public void initData() {
        EntityManager em = getEntityManager();

        // Revision 1
        em.getTransaction().begin();
        Person lukasz = new Person();
		lukasz.setName("Lukasz");
        lukasz.setGroup("IT");
        em.persist(lukasz);
        Role admin = new Role();
		admin.setName("Admin");
        admin.setGroup("Confidential");
        lukasz.getRoles().add(admin);
        admin.getMembers().add(lukasz);
        em.persist(admin);
        em.getTransaction().commit();

        expLukaszRev1 = new Person(lukasz.getId(), "IT", "Lukasz");
        expAdminRev1 = new Role(admin.getId(), "Confidential", "Admin");

        // Revision 2
        em.getTransaction().begin();
        lukasz = em.find(Person.class, lukasz.getId());
        lukasz.setGroup("Senior IT");
        lukasz.setName("Lukasz Antoniak");
        admin = em.find(Role.class, admin.getId());
        admin.setGroup("Very Confidential");
        em.getTransaction().commit();

        expAdminRev2 = new Role(admin.getId(), "Very Confidential", "Admin");
        expLukaszRev2 = new Person(lukasz.getId(), "Senior IT", "Lukasz Antoniak");

        // Revision 3
        em.getTransaction().begin();
        lukasz = em.find(Person.class, lukasz.getId());
        Role director = new Role();
		director.setName("Director");
        director.getMembers().add(lukasz);
        em.persist(director);
        lukasz.getRoles().add(director);
        em.getTransaction().commit();

        expDirectorRev3 = new Role(director.getId(), null, "Director");

        // Revision 4
        em.getTransaction().begin();
        Person adam = new Person();
        adam.setName("Adam");
        adam.setGroup("CEO");
        em.persist(adam);
        director = em.find(Role.class, director.getId());
        director.getMembers().add(adam);
        adam.getRoles().add(director);
        em.getTransaction().commit();

        expAdamRev4 = new Person(adam.getId(), "CEO", "Adam");

        // Revision 5
        em.getTransaction().begin();
        adam = em.find(Person.class, adam.getId());
        admin = em.find(Role.class, admin.getId());
        admin.getMembers().add(adam);
        em.getTransaction().commit();

        // Revision 6
        em.getTransaction().begin();
        adam = em.find(Person.class, adam.getId());
        adam.setName("Adam Warski");
        em.getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        Assert.assertEquals(Arrays.asList(1, 2, 3), getAuditReader().getRevisions(Person.class, expLukaszRev1.getId()));
        Assert.assertEquals(Arrays.asList(1, 2, 3), getAuditReader().getRevisions(RightsSubject.class, expLukaszRev1.getId()));

        Assert.assertEquals(Arrays.asList(4, 5, 6), getAuditReader().getRevisions(Person.class, expAdamRev4.getId()));
        Assert.assertEquals(Arrays.asList(4, 5, 6), getAuditReader().getRevisions(RightsSubject.class, expAdamRev4.getId()));

        Assert.assertEquals(Arrays.asList(1, 2, 5), getAuditReader().getRevisions(Role.class, expAdminRev1.getId()));
        Assert.assertEquals(Arrays.asList(1, 2, 5), getAuditReader().getRevisions(RightsSubject.class, expAdminRev1.getId()));

        Assert.assertEquals(Arrays.asList(3, 4), getAuditReader().getRevisions(Role.class, expDirectorRev3.getId()));
        Assert.assertEquals(Arrays.asList(3, 4), getAuditReader().getRevisions(RightsSubject.class, expDirectorRev3.getId()));
    }

    @Test
    public void testHistoryOfAdam() {
        Person adamRev4 = getAuditReader().find(Person.class, expAdamRev4.getId(), 4);
        RightsSubject rightsSubjectRev5 = getAuditReader().find(RightsSubject.class, expAdamRev4.getId(), 5);

        Assert.assertEquals(expAdamRev4, adamRev4);
        Assert.assertEquals(TestTools.makeSet(expDirectorRev3), adamRev4.getRoles());
        Assert.assertEquals(TestTools.makeSet(expDirectorRev3, expAdminRev2), rightsSubjectRev5.getRoles());
    }

    @Test
    public void testHistoryOfLukasz() {
        Person lukaszRev1 = getAuditReader().find(Person.class, expLukaszRev1.getId(), 1);
        Person lukaszRev2 = getAuditReader().find(Person.class, expLukaszRev1.getId(), 2);
        RightsSubject rightsSubjectRev3 = getAuditReader().find(RightsSubject.class, expLukaszRev1.getId(), 3);
        Person lukaszRev3 = getAuditReader().find(Person.class, expLukaszRev1.getId(), 3);

        Assert.assertEquals(expLukaszRev1, lukaszRev1);
        Assert.assertEquals(expLukaszRev2, lukaszRev2);
        Assert.assertEquals(TestTools.makeSet(expAdminRev1), lukaszRev1.getRoles());
        Assert.assertEquals(TestTools.makeSet(expAdminRev2, expDirectorRev3), rightsSubjectRev3.getRoles());
        Assert.assertEquals(TestTools.makeSet(expAdminRev2, expDirectorRev3), lukaszRev3.getRoles());
    }

    @Test
    public void testHistoryOfAdmin() {
        Role adminRev1 = getAuditReader().find(Role.class, expAdminRev1.getId(), 1);
        Role adminRev2 = getAuditReader().find(Role.class, expAdminRev1.getId(), 2);
        Role adminRev5 = getAuditReader().find(Role.class, expAdminRev1.getId(), 5);

        Assert.assertEquals(expAdminRev1, adminRev1);
        Assert.assertEquals(expAdminRev2, adminRev2);
        Assert.assertEquals(TestTools.makeSet(expLukaszRev1), adminRev1.getMembers());
        Assert.assertEquals(TestTools.makeSet(expLukaszRev2, expAdamRev4), adminRev5.getMembers());
    }
}
