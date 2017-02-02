package org.hibernate.jpa.test;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertThat;
import static org.hamcrest.core.Is.is;

@TestForIssue( jiraKey = "HHH-9029")
@RequiresDialectFeature(DialectChecks.SupportsRowValueConstructorSyntaxCheck.class)
public class CompositeIdRowValueTest extends BaseEntityManagerFunctionalTestCase {

    @Test
    public void testTupleAfterSubQuery() {
        EntityManager em = getOrCreateEntityManager();
        Query q = em.createQuery("SELECT e FROM EntityWithCompositeId e "
                + "WHERE EXISTS (SELECT 1 FROM EntityWithCompositeId) "
                + "AND e.id = :id");

        q.setParameter("id", new CompositeId(1, 2));

        assertThat(q.getResultList().size(), is(0));
    }

    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[] {EntityWithCompositeId.class, CompositeId.class};
    }

}
