/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination.hhh9965;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Created on 17/12/17.
 *
 * @author Reda.Housni-Alaoui
 */
@JiraKey(value = "HHH-9965")
public class HHH9965Test extends BaseCoreFunctionalTestCase {

	@Test
	public void testHHH9965() {
		Session session = openSession();
		session.beginTransaction();

		String hql = "SELECT s FROM Shop s join fetch s.products";

		try{
			session.createQuery(hql)
					.setMaxResults(3)
					.list();
			fail("Pagination over collection fetch failure was expected");
		} catch (Exception e){
			log.info(e.getMessage());
		}

		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure(cfg);
		cfg.setProperty( Environment.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH, true );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				Shop.class,
				Product.class
		};
	}
}
