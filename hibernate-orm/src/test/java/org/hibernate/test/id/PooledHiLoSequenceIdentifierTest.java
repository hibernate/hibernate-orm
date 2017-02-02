/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) {DATE}, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.id;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-9287")
public class PooledHiLoSequenceIdentifierTest extends BaseCoreFunctionalTestCase {
	private static final Logger log = Logger.getLogger( PooledHiLoSequenceIdentifierTest.class );

	@Test
	public void testSequenceIdentifierGenerator() {
		Session s = null;
		Transaction tx = null;
		try {
			s = openSession();
			tx = s.beginTransaction();

			for ( int i = 0; i < 5; i++ ) {
				s.persist( new SequenceIdentifier() );
			}
			s.flush();

			assertEquals( 5, countInsertedRows( s ) );

			insertNewRow( s );
			insertNewRow( s );

			assertEquals( 7, countInsertedRows( s ) );

			List<Number> ids = s.createQuery( "SELECT id FROM sequenceIdentifier" ).list();
			for ( Number id : ids ) {
				log.debug( "Found id: " + id );
			}

			for ( int i = 0; i < 3; i++ ) {
				s.persist( new SequenceIdentifier() );
			}
			s.flush();

			assertEquals( 10, countInsertedRows( s ) );
		}
		finally {
			if ( tx != null ) {
				tx.rollback();
			}

			s.close();
		}
	}

	private int countInsertedRows(Session s) {
		return ((Number) s.createSQLQuery( "SELECT COUNT(*) FROM sequenceIdentifier" )
				.uniqueResult()).intValue();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {SequenceIdentifier.class};
	}

	@Override
	protected void configure(Configuration configuration) {
		Properties properties = configuration.getProperties();
		properties.put( "hibernate.id.new_generator_mappings", "true" );
	}

	@Entity(name = "sequenceIdentifier")
	public static class SequenceIdentifier {

		@Id
		@GenericGenerator(name = "sampleGenerator", strategy = "enhanced-sequence",
				parameters = {
						@org.hibernate.annotations.Parameter(name = "optimizer", value = "pooled"),
						@org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
						@org.hibernate.annotations.Parameter(name = "increment_size", value = "2")
				}
		)
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "sampleGenerator")
		private Long id;
	}

	private void insertNewRow(Session session) {
		final SessionImplementor si = (SessionImplementor) session;
		final SessionFactoryImplementor sfi = si.getFactory();

		session.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						PreparedStatement statement = null;
						try {
							statement = connection.prepareStatement( "INSERT INTO sequenceIdentifier VALUES (?)" );
							statement.setObject( 1, sfi.getIdentifierGenerator( SequenceIdentifier.class.getName() ).generate( si, null ) );
							statement.executeUpdate();
						}
						finally {
							if ( statement != null ) {
								statement.close();
							}
						}
					}
				}
		);
	}
}

