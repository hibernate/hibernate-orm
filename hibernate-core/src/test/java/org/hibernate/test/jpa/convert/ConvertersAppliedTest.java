/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.jpa.convert;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceException;

import org.hibernate.Session;
import org.hibernate.test.jpa.AbstractJPATest;
import org.hibernate.testing.TestForIssue;
import org.jboss.logging.Logger;
import org.junit.Test;

/**
 * Test that converters are applied when they should.
 *
 * @author Etienne Miret
 */
public class ConvertersAppliedTest extends AbstractJPATest {

	private static final Logger logger = Logger.getLogger( ConvertersAppliedTest.class );

	@Override
	public String[] getMappings() {
		return new String[0];
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CannotBeStored.class, CannotBeRetrieved.class };
	}

	@Test
	public void testConverterOnStore() {
		final Session s = openSession();
		s.getTransaction().begin();
		try {

			final CannotBeStored obj = new CannotBeStored();
			obj.description = "Something";
			s.persist( obj );
			s.flush();
			fail("Converter was not applied on storing.");

		} catch (final PersistenceException e) {

			logger.info( "Persistence exception caught: " + e.getMessage() );
			assertNotNull( e.getCause() );
			assertTrue( e.getCause() instanceof Boom );

		} finally {
			s.getTransaction().rollback();
			s.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8697" )
	public void testConverterOnStoreNull() {
		final Session s = openSession();
		s.getTransaction().begin();
		try {

			final CannotBeStored obj = new CannotBeStored();
			s.persist( obj );
			s.flush();
			fail("Converter was not applied on storing null.");

		}
		catch (final PersistenceException e) {

			logger.info( "Persistence exception caught: " + e.getMessage() );
			assertNotNull( e.getCause() );
			assertTrue( e.getCause() instanceof Boom );

		}
		finally {
			s.getTransaction().rollback();
			s.close();
		}
	}

	@Test
	public void testConverterOnRetrieve() {
		final Session s = openSession();
		s.getTransaction().begin();
		try {

			final CannotBeRetrieved obj = new CannotBeRetrieved();
			obj.description = "Something";
			s.persist( obj );
			s.flush();
			s.clear();
			s.get( CannotBeRetrieved.class, obj.id );
			fail( "Converter not applied on retrieving." );

		}
		catch (final PersistenceException e) {

			logger.info( "Persistence exception caught: " + e.getMessage() );
			assertNotNull( e.getCause() );
			assertTrue( e.getCause() instanceof Boom );

		}
		finally {
			s.getTransaction().rollback();
			s.close();
		}
	}

	@Test
	public void testConverterOnRetrieveNull() {
		final Session s = openSession();
		s.getTransaction().begin();
		try {

			final CannotBeRetrieved obj = new CannotBeRetrieved();
			s.persist( obj );
			s.flush();
			s.clear();
			s.get( CannotBeRetrieved.class, obj.id );
			fail( "Converter not applied on retrieving null." );

		}
		catch (final PersistenceException e) {

			logger.info( "Persistence exception caught: " + e.getMessage() );
			assertNotNull( e.getCause() );
			assertTrue( e.getCause() instanceof Boom );

		}
		finally {
			s.getTransaction().rollback();
			s.close();
		}
	}

	public static class Boom extends RuntimeException {
		private static final long serialVersionUID = -2675867491813089539L;
	}

	public static class FailsToStore implements AttributeConverter<String, String> {

		@Override
		public String convertToDatabaseColumn(String attribute) {
			throw new Boom();
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			return dbData;
		}

	}

	public static class FailsToRetreive implements AttributeConverter<String, String> {

		@Override
		public String convertToDatabaseColumn(String attribute) {
			return attribute;
		}

		@Override
		public String convertToEntityAttribute(String dbData) {
			throw new Boom();
		}

	}

	@Entity
	public static class CannotBeStored {

		@Id
		@GeneratedValue
		public Long id;

		@Convert( converter = FailsToStore.class )
		public String description;

	}

	@Entity
	public static class CannotBeRetrieved {

		@Id
		@GeneratedValue
		public Long id;

		@Convert( converter = FailsToRetreive.class )
		public String description;

	}

}
