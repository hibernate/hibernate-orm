/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.derivedidentities.e4.a;

import java.util.Date;

import org.hibernate.Session;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.util.SchemaUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
//@FailureExpectedWithNewMetamodel(
//		jiraKey = "HHH-9048",
//		message = "Failures have to do with the lookups for MedicalHistory using its derived id (the simple Person id value)"
//)
public class DerivedIdentitySimpleParentSimpleDepTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testOneToOneExplicitJoinColumn() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK", metadata() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "MedicalHistory", "id", metadata() ) );

		Session s = openSession();
		s.getTransaction().begin();
		Person person = new Person( "aaa" );
		s.persist( person );
		MedicalHistory medicalHistory = new MedicalHistory( person );
		s.persist( medicalHistory );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		medicalHistory = (MedicalHistory) s.get( MedicalHistory.class, "aaa" );
		assertEquals( person.ssn, medicalHistory.patient.ssn );
		medicalHistory.lastupdate = new Date();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		medicalHistory = (MedicalHistory) s.get( MedicalHistory.class, "aaa" );
		assertNotNull( medicalHistory.lastupdate );
		s.delete( medicalHistory );
		s.delete( medicalHistory.patient );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testManyToOneExplicitJoinColumn() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "FinancialHistory", "patient_ssn", metadata() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "FinancialHistory", "id", metadata() ) );

		Session s = openSession();
		s.getTransaction().begin();
		Person person = new Person( "aaa" );
		s.persist( person );
		FinancialHistory financialHistory = new FinancialHistory( person );
		s.persist( financialHistory );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		financialHistory = (FinancialHistory) s.get( FinancialHistory.class, "aaa" );
		assertEquals( person.ssn, financialHistory.patient.ssn );
		financialHistory.lastUpdate = new Date();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		financialHistory = (FinancialHistory) s.get( FinancialHistory.class, "aaa" );
		assertNotNull( financialHistory.lastUpdate );
		s.delete( financialHistory );
		s.delete( financialHistory.patient );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSimplePkValueLoading() {
		Session s = openSession();
		s.getTransaction().begin();
		Person e = new Person( "aaa" );
		s.persist( e );
		FinancialHistory d = new FinancialHistory( e );
		s.persist( d );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		FinancialHistory history = (FinancialHistory) s.get( FinancialHistory.class, "aaa" );
		assertNotNull( history );
		s.delete( history );
		s.delete( history.patient );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				MedicalHistory.class,
				Simple.class,
				Person.class,
				FinancialHistory.class
		};
	}
}
