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
import java.util.ArrayList;
import java.util.List;

public class CollectionChangeNoRevisionTest extends AbstractSessionTest {

    protected static final int EXPECTED_PERSON_REVISION_COUNT = 1;
    protected static final String CREATE_REVISION_ON_COLLECTION_CHANGE = "false";
    protected Integer personId;

    @Override
    protected void initMappings() throws MappingException, URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("mappings/norevision/mappings.hbm.xml");
        config.addFile(new File(url.toURI()));
        config.setProperty("org.hibernate.envers.revision_on_collection_change", getCollectionChangeValue());
    }

    protected String getCollectionChangeValue() {
        return CREATE_REVISION_ON_COLLECTION_CHANGE;
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
    	newSessionFactory();
        Person p = new Person();
        Name n = new Name();
        n.setName("name1");
        p.getNames().add(n);
        getSession().getTransaction().begin();
        getSession().saveOrUpdate(p);
        getSession().getTransaction().commit();
        personId = p.getId();
        getSession().getTransaction().begin();
        n.setName("Changed name");
        getSession().saveOrUpdate(p);
        getSession().getTransaction().commit();
        getSession().getTransaction().begin();
        Name n2 = new Name();
        n2.setName("name2");
        p.getNames().add(n2);
        getSession().getTransaction().commit();
    }

    @Test
    public void testPersonRevisionCount() {
        int sizePerson = getAuditReader().getRevisions(Person.class, personId).size();
        assert sizePerson == getExpectedPersonRevisionCount();
    }

    protected int getExpectedPersonRevisionCount() {
        return EXPECTED_PERSON_REVISION_COUNT;
    }


}
