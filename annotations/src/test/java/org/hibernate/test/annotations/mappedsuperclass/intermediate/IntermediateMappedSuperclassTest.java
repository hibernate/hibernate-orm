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

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class IntermediateMappedSuperclassTest extends TestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { AccountBase.class, Account.class, SavingsAccountBase.class, SavingsAccount.class };
	}

	public void testGetOnIntermediateMappedSuperclass() {
		final BigDecimal withdrawalLimit = new BigDecimal( 1000 );
		Session session = openSession();
		session.beginTransaction();
		SavingsAccount savingsAccount = new SavingsAccount( "123", withdrawalLimit );
		session.save( savingsAccount );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		Account account = (Account) session.get( Account.class, savingsAccount.getId() );
		assertEquals( withdrawalLimit, ( (SavingsAccount) account ).getWithdrawalLimit() );
		session.delete( account );
		session.getTransaction().commit();
		session.close();
	}
}
