/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import java.math.BigDecimal;
import java.util.Map;
import jakarta.validation.ConstraintViolationException;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class BeanValidationDisabledTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testListeners() {
		CupHolder ch = new CupHolder();
		ch.setRadius( new BigDecimal( "12" ) );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		try {
			s.persist( ch );
			s.flush();
		}
		catch ( ConstraintViolationException e ) {
			fail( "invalid object should not be validated" );
		}
		tx.rollback();
		s.close();
	}

	@Test
	public void testDDLDisabled() {
		PersistentClass classMapping = metadata().getEntityBinding( Address.class.getName() );
		Column countryColumn = (Column) classMapping.getProperty( "country" ).getSelectables().get( 0 );
		assertTrue( "DDL constraints are applied", countryColumn.isNullable() );
	}

	@Override
	protected void addSettings(Map<String,Object> settings) {
		settings.put( "jakarta.persistence.validation.mode", "none" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Address.class,
				CupHolder.class
		};
	}
}
