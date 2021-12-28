package org.hibernate.orm.test.mapping.generated.sqldefault;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * @author Gavin King
 */
@DomainModel(annotatedClasses = DefaultTest.OrderLine.class)
@SessionFactory
public class DefaultTest {

    @Test
    public void test(SessionFactoryScope scope) {
        scope.inTransaction( session -> {
            BigDecimal unitPrice = new BigDecimal("12.99");
            OrderLine entity = new OrderLine( unitPrice, 5 );
            session.persist(entity);
            session.flush();
            assertEquals( "new", entity.status );
        } );
    }

    @AfterEach
    public void dropTestData(SessionFactoryScope scope) {
        scope.inTransaction( session -> session.createQuery( "delete WithDefault" ).executeUpdate() );
    }

    @Entity(name="WithDefault")
    public static class OrderLine {
        @Id
        private BigDecimal unitPrice;
        @Id
        private int quantity;
        @ColumnDefault(value = "'new'")
        private String status;

        public OrderLine() {}
        public OrderLine(BigDecimal unitPrice, int quantity) {
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }
    }
}
