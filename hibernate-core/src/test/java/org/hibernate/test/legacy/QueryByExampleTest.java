//$Id: QueryByExampleTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.legacy;

import java.util.List;

import junit.framework.Test;

import org.hibernate.Criteria;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * Query by example test to allow nested components
 *
 * @author Emmanuel Bernard
 */
public class QueryByExampleTest extends LegacyTestCase {

    public QueryByExampleTest(String name) {
        super(name);
    }

    public String[] getMappings() {
        return new String[] { "legacy/Componentizable.hbm.xml" };
    }

	public static Test suite() {
		return new FunctionalTestClassTestSuite( QueryByExampleTest.class );
	}

    public void testSimpleQBE() throws Exception {
    	deleteData();
        initData();

        Session s = openSession();

        Transaction t = s.beginTransaction();
        Componentizable master = getMaster("hibernate", "open sourc%", "open source1");
        Criteria crit = s.createCriteria(Componentizable.class);
        Example ex = Example.create(master).enableLike();
        crit.add(ex);
        List result = crit.list();
        assertNotNull(result);
        assertEquals(1, result.size());

        t.commit();
        s.close();
    }

    public void testJunctionNotExpressionQBE() throws Exception {
        deleteData();
        initData();
        Session s = openSession();
        Transaction t = s.beginTransaction();
        Componentizable master = getMaster("hibernate", null, "ope%");
        Criteria crit = s.createCriteria(Componentizable.class);
        Example ex = Example.create(master).enableLike();

        crit.add(Restrictions.or(Restrictions.not(ex), ex));

        List result = crit.list();
        assertNotNull(result);
        assertEquals(2, result.size());
        t.commit();
        s.close();

    }

    public void testExcludingQBE() throws Exception {
        deleteData();
        initData();
        Session s = openSession();
        Transaction t = s.beginTransaction();
        Componentizable master = getMaster("hibernate", null, "ope%");
        Criteria crit = s.createCriteria(Componentizable.class);
        Example ex = Example.create(master).enableLike()
            .excludeProperty("component.subComponent");
        crit.add(ex);
        List result = crit.list();
        assertNotNull(result);
        assertEquals(3, result.size());

        master = getMaster("hibernate", "ORM tool", "fake stuff");
        crit = s.createCriteria(Componentizable.class);
        ex = Example.create(master).enableLike()
            .excludeProperty("component.subComponent.subName1");
        crit.add(ex);
        result = crit.list();
        assertNotNull(result);
        assertEquals(1, result.size());
        t.commit();
        s.close();


    }

    private void initData() throws Exception {
        Session s = openSession();
        Transaction t = s.beginTransaction();
        Componentizable master = getMaster("hibernate", "ORM tool", "ORM tool1");
        s.saveOrUpdate(master);
        master = getMaster("hibernate", "open source", "open source1");
        s.saveOrUpdate(master);
        master = getMaster("hibernate", null, null);
        s.saveOrUpdate(master);
        t.commit();
        s.close();
    }

    private void deleteData() throws Exception {
    	Session s = openSession();
        Transaction t = s.beginTransaction();
        s.delete("from Componentizable");
        t.commit();
        s.close();
    }

    private Componentizable getMaster(String name, String subname, String subname1) {
        Componentizable master = new Componentizable();
        if (name != null) {
            Component masterComp = new Component();
            masterComp.setName(name);
            if (subname != null || subname1 != null) {
                SubComponent subComponent = new SubComponent();
                subComponent.setSubName(subname);
                subComponent.setSubName1(subname1);
                masterComp.setSubComponent(subComponent);
            }
            master.setComponent(masterComp);
        }
        return master;
    }
}
