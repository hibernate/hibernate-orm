package org.hibernate.orm.test.mapping.generated.always;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * @author Gavin King
 */
@DomainModel(annotatedClasses = GeneratedAlwaysTest.OrderLine.class)
@SessionFactory
@SkipForDialect(dialectClass = H2Dialect.class) // 'generated always' not supported until H2 2.0
@SkipForDialect(dialectClass = HSQLDialect.class)
@SkipForDialect(dialectClass = DerbyDialect.class)
@SkipForDialect(dialectClass = SybaseASEDialect.class)
@SkipForDialect(dialectClass = PostgreSQLDialect.class, majorVersion = 9)
public class GeneratedAlwaysTest {

    @Test
    public void test(SessionFactoryScope scope) {
        scope.inTransaction( session -> {
            BigDecimal unitPrice = new BigDecimal("12.99");
            OrderLine entity = new OrderLine( unitPrice, 5, 10 );
            session.persist(entity);
            session.flush();
            assertEquals( 5, entity.quantity );
            assertEquals( unitPrice, entity.unitPrice );
            assertEquals( unitPrice.multiply( new BigDecimal("5") ), entity.total );
            assertEquals( 58, entity.discounted.intValue() );
        } );
    }

    @AfterEach
    public void dropTestData(SessionFactoryScope scope) {
        scope.inTransaction( session -> session.createQuery( "delete WithGeneratedAlways" ).executeUpdate() );
    }

    @Entity(name="WithGeneratedAlways")
    public static class OrderLine {
        @Id
        private BigDecimal unitPrice;
        @Id
        private int quantity;
        private int discount;
        @GeneratedColumn(value = "unitPrice*quantity", fetch = true)
        private BigDecimal total;
        @Column(name = "discountedTotal")
        @GeneratedColumn(name = "discountedTotal", value = "unitPrice*quantity*(1.0-discount/100.0)", fetch = true)
        private BigDecimal discounted;

        public OrderLine() {}
        public OrderLine(BigDecimal unitPrice, int quantity, int discount) {
            this.unitPrice = unitPrice;
            this.quantity = quantity;
            this.discount = discount;
        }
    }
}
