package org.hibernate.test.model.orders;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

public class ModelTwoPurchaseEntityTest extends BaseEntityManagerFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedClasses() {
    return new Class<?>[]{org.hibernate.test.model.orders.current.Purchase.class, org.hibernate.test.model.orders.old.Purchase.class};
  }

  @Test
  public void test() {
    EntityManager em = getOrCreateEntityManager();
    em.getTransaction().begin();
    CriteriaBuilder cb = em.getCriteriaBuilder();

    CriteriaQuery<org.hibernate.test.model.orders.current.Purchase> cq = cb
        .createQuery(org.hibernate.test.model.orders.current.Purchase.class);
    cq.from(org.hibernate.test.model.orders.current.Purchase.class);
    log.info("Must select from current");
    em.createQuery(cq).getResultList();

    CriteriaQuery<org.hibernate.test.model.orders.old.Purchase> cq1 = cb.createQuery(org.hibernate.test.model.orders.old.Purchase.class);
    cq1.from(org.hibernate.test.model.orders.old.Purchase.class);
    log.info("Must select from old");
    em.createQuery(cq1).getResultList();

    em.getTransaction().commit();
    em.close();
  }
}
