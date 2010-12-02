package org.hibernate.envers.test.integration.collection.norevision;

import org.hibernate.MappingException;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.test.AbstractSessionTest;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class CollectionChangeNoRevisionTest extends AbstractSessionTest {

    private Integer personId;

    @Override
    protected void initMappings() throws MappingException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("mappings/norevision/mappings.hbm.xml");
        config.addFile(new File(url.toURI()));
        config.setProperty("org.hibernate.envers.revision_on_collection_change", "false");
    }

    @BeforeMethod(firstTimeOnly = true)
    public void initData() {
    	newSessionFactory();
        Person p = new Person();
        Name n = new Name();
        n.setName("name1");
        p.getNames().add(n);
        Transaction transaction = getSession().beginTransaction();
        getSession().saveOrUpdate(p);
        transaction.commit();
        personId = p.getId();
        System.err.print(p);

    }

    @Test
    public void testPersonRevisionCount() {
        Person p = (Person) getSession().createCriteria(Person.class).add(Restrictions.idEq(personId)).uniqueResult();
        Name n2 = new Name();
        n2.setName("name2");
        p.getNames().add(n2);
        Transaction transaction = getSession().beginTransaction();
        getSession().saveOrUpdate(p);
        transaction.commit();
        int size = getAuditReader().getRevisions(Person.class, personId).size();
        System.out.println(size);
        Assert.assertEquals(config.getProperty("org.hibernate.envers.revision_on_collection_change"), "false");
        Assert.assertEquals(size, 1);
    }


}
