/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.paths;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
        Order.class,
        LineItem.class
})
public class HHH16864Test {

    @Test
    public void testDistinctCollectionJoin(EntityManagerFactoryScope scope) {
        scope.inTransaction(
                entityManager -> {
                    prepareData(entityManager);
                    CriteriaQuery<Order> criteria = createCriteria(entityManager);

                    criteria.distinct(true);
                    TypedQuery<Order> query = entityManager.createQuery(criteria);
                    List<Order> orders = query.getResultList();

                    assertThat(orders, hasSize(1));
                }
        );
    }

    private static CriteriaQuery<Order> createCriteria(EntityManager entityManager) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> criteria = criteriaBuilder.createQuery(Order.class);
        Root<Order> orderRoot = criteria.from(Order.class);
        Join<Order, LineItem> lineItemsJoin = orderRoot.join(Order_.lineItems);
        criteria.where(criteriaBuilder.ge(lineItemsJoin.get(LineItem_.quantity), 2));
        criteria.select(orderRoot);
        return criteria;
    }

    private static void prepareData(EntityManager entityManager) {
        Order order = new Order(UUID.randomUUID().toString(), 1337.42);
        LineItem lineItem1 = new LineItem(UUID.randomUUID().toString(), 1, order);
        LineItem lineItem2 = new LineItem(UUID.randomUUID().toString(), 2, order);
        LineItem lineItem3 = new LineItem(UUID.randomUUID().toString(), 3, order);
        order.setLineItems(List.of(lineItem1, lineItem2, lineItem3));
        entityManager.persist(lineItem1);
        entityManager.persist(lineItem2);
        entityManager.persist(lineItem3);
        entityManager.persist(order);
    }

    @Test
    public void testNonDistinctCollectionJoin(EntityManagerFactoryScope scope) {
        scope.inTransaction(
                entityManager -> {
                    prepareData(entityManager);
                    CriteriaQuery<Order> criteria = createCriteria(entityManager);

                    criteria.distinct(false);
                    TypedQuery<Order> query = entityManager.createQuery(criteria);

                    // SQL statement returns two rows, one for each line item with quantity >= 2 joined to the same order
                    assertThat(query.getResultList(), hasSize(2));
                }
        );
    }
}
