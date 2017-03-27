package org.hibernate.query.criteria.internal.expression;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;

import java.util.List;

/**
 *
 * @author Vasyl Danyliuk
 */
public class SearchedCaseExpressionTest extends BaseCoreFunctionalTestCase {

    @Test
    public void testSharedTransactionContextSessionClosing() {
        EntityManager entityManager = openSession();
        entityManager.getTransaction().begin();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Event> criteria = cb.createQuery(Event.class);

        Root<Event> event = criteria.from(Event.class);
        Path<EventType> type = event.get("type");

        Expression<String> caseWhen = cb.<EventType, String>selectCase(type)
                .when(EventType.TYPE1, "Admin Event")
                .when(EventType.TYPE2, "User Event")
                .when(EventType.TYPE3, "Reporter Event")
                .otherwise("");

        criteria.select(event);
        criteria.where(cb.equal(caseWhen, "Admin Event")); // OK when use cb.like() method and others
        List<Event> resultList = entityManager.createQuery(criteria).getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        Assert.assertNotNull(resultList);
    }

    @Test
    public void testSharedTransactionContextSession() {
        EntityManager entityManager = openSession();
        entityManager.getTransaction().begin();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Event> criteria = cb.createQuery(Event.class);

        Root<Event> event = criteria.from(Event.class);
        Path<EventType> type = event.get("type");

        Expression<String> caseWhen = cb.<String>selectCase()
                .when(cb.equal(type, EventType.TYPE1), "Type1")
                .otherwise("");

        criteria.select(event);
        criteria.where(cb.equal(caseWhen, "Admin Event")); // OK when use cb.like() method and others
        List<Event> resultList = entityManager.createQuery(criteria).getResultList();

        entityManager.getTransaction().commit();
        entityManager.close();

        Assert.assertNotNull(resultList);
    }

    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[]{Event.class, EventType.class};
    }
}