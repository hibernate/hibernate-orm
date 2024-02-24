package org.hibernate.orm.test.jpa.criteria.fetch;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FetchAssertTest extends BaseEntityManagerFunctionalTestCase {

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class[] { ThirdParty.class, ThirdPartyAuth.class, Agent.class, BillingParty.class };
    }

    @Before
    public void setup() {
        doInJPA(this::entityManagerFactory, em -> {
            final ThirdParty thirdParty = new ThirdParty();
            thirdParty.setName("Tester Third Party");
            em.persist(thirdParty);

            final BillingParty billingParty = new BillingParty();
            billingParty.setThirdParty(thirdParty);
            thirdParty.setBillingParty(billingParty);
            em.persist(billingParty);

            final Agent agent = new Agent();
            agent.setThirdParty(thirdParty);
            thirdParty.setAgent(agent);
            em.persist(agent);

            final ThirdPartyAuth thirdPartyAuth = new ThirdPartyAuth();
            thirdPartyAuth.setThirdParty(thirdParty);
            em.persist(thirdPartyAuth);
        });
    }

    @Test
    @JiraKey("HHH-17777")
    public void reusingFetch() {
        EntityManager em = getOrCreateEntityManager();
        CriteriaBuilder builder = em.getCriteriaBuilder();

        CriteriaQuery<ThirdPartyAuth> query = builder.createQuery(ThirdPartyAuth.class);
        Root<ThirdPartyAuth> root = query.from(ThirdPartyAuth.class);
        Fetch<ThirdPartyAuth, ThirdParty> thirdPartyFetch = root.fetch("thirdParty", JoinType.INNER);
        thirdPartyFetch.fetch("agent", JoinType.LEFT);
        thirdPartyFetch.fetch("billingParty", JoinType.LEFT);
        List<ThirdPartyAuth> results = em.createQuery(query).getResultList();
        Assert.assertEquals(1, results.size());
    }

    @Test
    @JiraKey("HHH-17777")
    public void fetchingFromRootTwice() {
        EntityManager em = getOrCreateEntityManager();
        CriteriaBuilder builder = em.getCriteriaBuilder();

        CriteriaQuery<ThirdPartyAuth> query = builder.createQuery(ThirdPartyAuth.class);
        Root<ThirdPartyAuth> root = query.from(ThirdPartyAuth.class);
        root.fetch("thirdParty", JoinType.INNER).fetch("agent", JoinType.LEFT);
        root.fetch("thirdParty", JoinType.INNER).fetch("billingParty", JoinType.LEFT);
        List<ThirdPartyAuth> results = em.createQuery(query).getResultList();
        Assert.assertEquals(1, results.size());
    }
}
