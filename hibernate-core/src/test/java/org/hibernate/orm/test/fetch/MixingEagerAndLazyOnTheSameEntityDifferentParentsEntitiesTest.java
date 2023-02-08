/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.fetch;

import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.*;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@RunWith(BytecodeEnhancerRunner.class)
@TestForIssue(jiraKey = "HHH-16136")
public class MixingEagerAndLazyOnTheSameEntityDifferentParentsEntitiesTest extends BaseEntityManagerFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{
                User.class,
                Order.class,
                OrderItem.class
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void addConfigOptions(Map options) {
        options.put(AvailableSettings.CLASSLOADERS, getClass().getClassLoader());
    }

    @Before
    public void prepare() {
        doInJPA(this::entityManagerFactory, em -> {
            User user = new User("User 1");
            user.setName("User 1 name");

            User targetUser = new User("User 2");
            targetUser.setName("User 2 name");

            Order order = new Order("Order 1");
            order.setUser(user);
            order.setTargetUser(targetUser);

            OrderItem orderItem = new OrderItem("Order Item 1");
            orderItem.setUser(user);
            orderItem.setOrder(order);
            order.orderItems.add(orderItem);

            em.persist(user);
            em.persist(targetUser);
            em.persist(order);
            em.persist(orderItem);
        });
    }

    @Test
    public void testFetchingMultipleOrdersWorks() {
        Order order = doInJPA(this::entityManagerFactory, em -> {
            Order orderLocal = em.createQuery("select o from Order o", Order.class)
                    .getResultList()
                    .get(0);
            return orderLocal;
        });
        assertEquals(1, order.orderItems.size());
        assertEquals("User 1", order.getUser().getId());
        assertEquals("User 1 name", order.getUser().getName());
    }

    @Test
    public void testFetchingSingleOrderWorks() {
        Order order = doInJPA(this::entityManagerFactory, em -> {
            Order orderLocal = em.find(Order.class, "Order 1");
            return orderLocal;
        });
        assertEquals(1, order.orderItems.size());
        assertEquals("User 1", order.getUser().getId());
        assertEquals("User 1 name", order.getUser().getName());
    }


    @Entity(name = "Order")
    @Table(name = "Orders")
    public static class Order {

        @Id
        private String id;

        private Set<OrderItem> orderItems = new HashSet<>();

        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn
        private User user = null;


        /**
         * The presence of this targetUser field makes Hibernate fetch Order and its relationships in several queries
         * instead of one query, which then causes this test to fail.
         */
        @ManyToOne(fetch = FetchType.EAGER)
        @JoinColumn
        private User targetUser = null;

        public Order(String id) {
            this();
            setId(id);
        }

        protected Order() {
        }

        @SuppressWarnings("unused")
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @OneToMany(targetEntity = OrderItem.class, mappedBy = "order", fetch = FetchType.EAGER)
        @Access(AccessType.PROPERTY)
        @SuppressWarnings("unused")
        public Set<OrderItem> getOrderItems() {
            if (orderItems == null) {
                orderItems = new HashSet<>();
            }
            return orderItems;
        }

        @SuppressWarnings("unused")
        public void setOrderItems(Set<OrderItem> pOrderItems) {
            orderItems = pOrderItems;
        }

        @SuppressWarnings("unused")
        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        @SuppressWarnings("unused")
        public User getTargetUser() {
            return targetUser;
        }

        public void setTargetUser(User targetUser) {
            this.targetUser = targetUser;
        }
    }


    @Entity(name = "User")
    @Table(name = "Users")
    public static class User {

        @Id
        private String id;

        private String name;

        public User(String id) {
            this();
            setId(id);
        }

        protected User() {
        }

        @SuppressWarnings("unused")
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @SuppressWarnings("unused")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Entity(name = "OrderItem")
    @Table(name = "OrderItems")
    public static class OrderItem {

        @Id
        private String id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn
        private User user = null;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn
        private Order order = null;

        @SuppressWarnings("unused")
        protected OrderItem() {
        }

        public OrderItem(String id) {
            setId(id);
        }

        @SuppressWarnings("unused")
        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        @SuppressWarnings("unused")
        public Order getOrder() {
            return order;
        }

        public void setOrder(Order order) {
            this.order = order;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
