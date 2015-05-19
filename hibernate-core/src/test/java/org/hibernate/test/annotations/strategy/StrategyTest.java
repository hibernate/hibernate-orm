/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.strategy;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class StrategyTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testComponentSafeStrategy() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Location start = new Location();
		start.setCity( "Paris" );
		start.setCountry( "France" );
		Location end = new Location();
		end.setCity( "London" );
		end.setCountry( "UK" );
		Storm storm = new Storm();
		storm.setEnd( end );
		storm.setStart( start );
		s.persist( storm );
		s.flush();
		tx.rollback();
		s.close();
	}

	@Override
	protected void configureMetadataBuilder(MetadataBuilder metadataBuilder) {
		super.configureMetadataBuilder( metadataBuilder );
		metadataBuilder.applyImplicitNamingStrategy( ImplicitNamingStrategyComponentPathImpl.INSTANCE );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Storm.class };
	}
}
