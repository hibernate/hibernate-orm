package org.hibernate.envers.test.integration.collection.norevision;
import java.net.URISyntaxException;
import java.util.List;
import org.hibernate.MappingException;
import org.hibernate.envers.test.AbstractSessionTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public abstract class AbstractCollectionChangeTest extends AbstractSessionTest {
    protected Integer personId;

    @Override
    protected void initMappings() throws MappingException, URISyntaxException {
        config.addAnnotatedClass(Person.class);
        config.addAnnotatedClass(Name.class);
        config.setProperty("org.hibernate.envers.revision_on_collection_change", getCollectionChangeValue());
    }

    protected abstract String getCollectionChangeValue();

    protected abstract List<Integer> getExpectedPersonRevisions();

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
    	newSessionFactory();

        // Rev 1
        getSession().getTransaction().begin();        
        Person p = new Person();
        Name n = new Name();
        n.setName("name1");
        p.getNames().add(n);
        getSession().saveOrUpdate(p);
        getSession().getTransaction().commit();

        // Rev 2
        getSession().getTransaction().begin();
        n.setName("Changed name");
        getSession().saveOrUpdate(p);
        getSession().getTransaction().commit();

        // Rev 3
        getSession().getTransaction().begin();
        Name n2 = new Name();
        n2.setName("name2");
        p.getNames().add(n2);
        getSession().getTransaction().commit();

        personId = p.getId();
    }

    @Test
    public void testPersonRevisionCount() {
        assert getAuditReader().getRevisions(Person.class, personId).equals(getExpectedPersonRevisions());
    }
}
