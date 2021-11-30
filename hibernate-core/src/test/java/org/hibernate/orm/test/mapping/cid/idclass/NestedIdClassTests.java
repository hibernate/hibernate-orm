/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.cid.idclass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = {
		Customer.class,
		Order.class,
		Payment.class
})
@SessionFactory
public class NestedIdClassTests {
	@Test
	public void smokeTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String qry = "from Payment p join fetch p.id.order o join fetch o.id.customer";
			session.createQuery( qry ).list();
		});
		scope.inTransaction( (session) -> {
			final String qry = "from Payment p join fetch p.order o join fetch o.customer";
			session.createQuery( qry ).list();
		});
	}

	@Test
	public void smokeTest2(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String qry = "from Payment p";
			final Payment payment = session.createQuery( qry, Payment.class ).uniqueResult();
			assertThat( payment ).isNotNull();
			assertThat( payment.accountNumber ).isNotNull();
			assertThat( payment.order ).isNotNull();

			assertThat( payment.order.orderNumber ).isNotNull();
			assertThat( payment.order.customer ).isNotNull();

			assertThat( payment.order.customer.id ).isNotNull();
			assertThat( payment.order.customer.name ).isNotNull();
		});
		scope.inTransaction( (session) -> {
			final String qry = "from Payment p join fetch p.order o join fetch o.customer";
			session.createQuery( qry ).list();
		});
	}

	@Test
	public void smokeTest3(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Payment payment = session.get( Payment.class, new PaymentId( new OrderId( 1, 1 ), "123" ) );
			assertThat( payment ).isNotNull();
			assertThat( payment.accountNumber ).isNotNull();
			assertThat( payment.order ).isNotNull();

			assertThat( payment.order.orderNumber ).isNotNull();
			assertThat( payment.order.customer ).isNotNull();

			assertThat( payment.order.customer.id ).isNotNull();
			assertThat( payment.order.customer.name ).isNotNull();
		});
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Customer acme = new Customer( 1, "acme" );
			final Customer spacely = new Customer( 2, "spacely" );
			session.persist( acme );
			session.persist( spacely );

			final Order acmeOrder1 = new Order( acme, 1, 123F );
			final Order acmeOrder2 = new Order( acme, 2, 123F );
			session.persist( acmeOrder1 );
			session.persist( acmeOrder2 );

			final Payment payment = new Payment( acmeOrder1, "123" );
			session.persist( payment );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "delete Payment" ).executeUpdate();
			session.createQuery( "delete Order" ).executeUpdate();
			session.createQuery( "delete Customer" ).executeUpdate();
		} );
	}
}
