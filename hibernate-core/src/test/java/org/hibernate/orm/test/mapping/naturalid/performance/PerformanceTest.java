package org.hibernate.orm.test.mapping.naturalid.performance;

import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.testing.orm.junit.*;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.function.Consumer;

import static jakarta.persistence.GenerationType.AUTO;

@ServiceRegistry(settings = {
        @Setting(name = AvailableSettings.DISABLE_NATURAL_ID_RESOLUTIONS_CACHE, value = "true")
})
@SessionFactory
@DomainModel(annotatedClasses = {PerformanceTest.Node.class})
public class PerformanceTest {
    @Test
    public void testIt(SessionFactoryScope sessionFactory) {
        sessionFactory.inTransaction(new Consumer<SessionImplementor>() {
            @Override
            public void accept(SessionImplementor session) {
                var node = new Node();
                for (int i = 0; i < 100_000; i++) {
                    session.persist(node);
                    final var newNode = new Node();
                    newNode.nextNode = node;
                    node = newNode;
                }
            }
        });
        sessionFactory.inSession(new Consumer<SessionImplementor>() {
            @Override
            public void accept(SessionImplementor sessionImplementor) {
                final var selectXFromNode = "from Node";
                final var query = sessionImplementor.createQuery(selectXFromNode);
                query.getResultList();
            }
        });
    }


    @Entity(name = "Node")
    public static class Node {
        @Id
        @GeneratedValue(strategy = AUTO)
        public int ident;
        @ManyToOne(fetch = FetchType.LAZY)
        @NaturalId
        public Node nextNode;
        @NaturalId
        public String otherPart;


        public Node getNextNode() {
            return nextNode;
        }

        public String getOtherPart() {
            return otherPart;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Node node)) return false;
            if (getNextNode() != null ? !getNextNode().equals(node.getNextNode()) : node.getNextNode() != null)
                return false;
            return getOtherPart() != null ? getOtherPart().equals(node.getOtherPart()) : node.getOtherPart() == null;
        }

        @Override
        public int hashCode() {
            int result = getNextNode() != null ? getNextNode().hashCode() : 0;
            result = 31 * result + (getOtherPart() != null ? getOtherPart().hashCode() : 0);
            return result;
        }
    }
}
