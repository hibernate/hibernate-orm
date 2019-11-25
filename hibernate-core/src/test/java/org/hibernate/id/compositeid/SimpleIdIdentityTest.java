package org.hibernate.id.compositeid;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class SimpleIdIdentityTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SimpleIdIdentityEntity.class };
	}

	@Test
	public void test() {
		doInHibernate( this::sessionFactory, session -> {
			SimpleIdIdentityEntity entity = new SimpleIdIdentityEntity();
			session.persist( entity );
			assertNotNull( entity.getId() );
		} );
	}
}
