/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.mappedsuperclass.intermediate;

import java.math.BigDecimal;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class IntermediateMappedSuperclassTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { AccountBase.class, Account.class, SavingsAccountBase.class, SavingsAccount.class };
	}

	@Test
	public void testGetOnIntermediateMappedSuperclass() {
		final BigDecimal withdrawalLimit = new BigDecimal( 1000.00 ).setScale( 2 );
		Session session = openSession();
		session.beginTransaction();
		SavingsAccount savingsAccount = new SavingsAccount( "123", withdrawalLimit );
		session.save( savingsAccount );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		Account account = (Account) session.get( Account.class, savingsAccount.getId() );
		// Oracle returns the BigDecimal with scale=0, which is equal to 1000 (not 1000.00);
		// compare using BigDecimal.doubleValue;
		assertEquals(
				withdrawalLimit.doubleValue(),
				( (SavingsAccount) account ).getWithdrawalLimit().doubleValue(),
				0.001);
		session.delete( account );
		session.getTransaction().commit();
		session.close();
	}
}
