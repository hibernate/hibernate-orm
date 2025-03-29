/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.immutable;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import java.lang.invoke.MethodHandles;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@JiraKey( value = "HHH-12387" )
public class ImmutableEntityUpdateQueryHandlingModeWarningTest extends BaseNonConfigCoreFunctionalTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, SqmUpdateStatement.class.getName() ) );

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Country.class, State.class, Photo.class };
	}

	@Test
	public void testBulkUpdate(){
		Country _country = doInHibernate( this::sessionFactory, session -> {
			Country country = new Country();
			country.setName("Germany");
			session.persist(country);

			return country;
		} );

		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000487" );
		triggerable.reset();

		doInHibernate( this::sessionFactory, session -> {
			session.createQuery(
				"update Country " +
				"set name = :name" )
			.setParameter( "name", "N/A" )
			.executeUpdate();
		} );

		assertEquals( "HHH000487: The query: [update Country set name = :name] attempts to update an immutable entity: [Country]", triggerable.triggerMessage() );

		doInHibernate( this::sessionFactory, session -> {
			Country country = session.find(Country.class, _country.getId());
			assertEquals( "N/A", country.getName() );
		} );
	}
}
