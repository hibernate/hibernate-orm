package org.hibernate.orm.test.query.hhh18291;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.TypedQuery;

@DomainModel(
		annotatedClasses = {
				InvoiceBE.class,
				PaidInvoiceBE.class
		}
)
@SessionFactory
public class BooleanSubqueryTest {

	@Test
	@JiraKey("HHH-18292")
	public void hhh18291Test(SessionFactoryScope scope) throws Exception {
		final var paidInvoiceBE = scope.fromTransaction( entityManager -> {
			final var invoice = new InvoiceBE().setId( 1L ).setRemoved( false );
			entityManager.persist( invoice );
			final var paidInvoice = new PaidInvoiceBE().setId( 1 ).setInvoice( invoice );
			entityManager.persist( paidInvoice );
			return paidInvoice;
		} );
		scope.inTransaction( entityManager -> {
			TypedQuery<Boolean> query = entityManager.createQuery(
					"SELECT i.removed = false " +
					"AND (SELECT count(*) = 0 " +
					"FROM PaidInvoiceBE pi " +
					"WHERE pi.invoice.id = i.id) " +
					"FROM InvoiceBE i " +
					"WHERE i.id = :invoiceId", Boolean.class );
			query.setParameter( "invoiceId", 1L );
			Boolean result = query.getSingleResult();
			Assertions.assertFalse( result );
		} );
	}
}
