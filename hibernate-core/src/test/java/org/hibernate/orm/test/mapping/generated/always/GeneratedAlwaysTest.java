package org.hibernate.orm.test.mapping.generated.always;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.ColumnGeneratedAlways;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * @author Gavin King
 */
@DomainModel(annotatedClasses = GeneratedAlwaysTest.OrderLine.class)
@SessionFactory
@SkipForDialect(dialectClass = H2Dialect.class) // 'generated always' not supported until H2 2.0
public class GeneratedAlwaysTest {

    @Test
    public void test(SessionFactoryScope scope) {
        scope.inTransaction( session -> {
            BigDecimal unitPrice = new BigDecimal("12.99");
            OrderLine entity = new OrderLine( unitPrice, 5 );
            session.persist(entity);
            session.flush();
            assertEquals( unitPrice.multiply( new BigDecimal("5") ), entity.total );
        } );
    }

    @Entity(name="WithGeneratedAlways")
    public static class OrderLine {
        @Id
        private BigDecimal unitPrice;
        @Id
        private int quantity;
        @ColumnGeneratedAlways(value = "unitPrice*quantity")
        private BigDecimal total;

        public OrderLine() {}
        public OrderLine(BigDecimal unitPrice, int quantity) {
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }
    }
}
