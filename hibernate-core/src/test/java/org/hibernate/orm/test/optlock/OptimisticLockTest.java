/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.optlock;


import jakarta.persistence.PersistenceException;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.DialectContext.getDialect;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests relating to the optimistic-lock mapping option.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@RequiresDialectFeature(
		feature = DialectFeatureChecks.DoesRepeatableReadCauseReadersToBlockWritersCheck.class, reverse = true,
		comment = "potential deadlock"
)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/optlock/Document.hbm.xml"
)
@SessionFactory
public class OptimisticLockTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testOptimisticLockDirty(SessionFactoryScope scope) {
		testUpdateOptimisticLockFailure( "LockDirty", scope );
	}

	@Test
	public void testOptimisticLockAll(SessionFactoryScope scope) {
		testUpdateOptimisticLockFailure( "LockAll", scope );
	}

	@Test
	public void testOptimisticLockDirtyDelete(SessionFactoryScope scope) {
		testDeleteOptimisticLockFailure( "LockDirty", scope );
	}

	@Test
	public void testOptimisticLockAllDelete(SessionFactoryScope scope) {
		testDeleteOptimisticLockFailure( "LockAll", scope );
	}

	private void testUpdateOptimisticLockFailure(String entityName, SessionFactoryScope scope) {
		Document doc = scope.fromTransaction(
				session -> {
					Document document = new Document();
					document.setTitle( "Hibernate in Action" );
					document.setAuthor( "Bauer et al" );
					document.setSummary( "Very boring book about persistence" );
					document.setText( "blah blah yada yada yada" );
					document.setPubDate( new PublicationDate( 2004 ) );
					session.persist( entityName, document );
					return document;
				}
		);

		scope.inTransaction(
				mainSession -> {
					Document document = (Document) mainSession.get( entityName, doc.getId() );

					scope.inTransaction(
							otherSession -> {
								Document otherDoc = (Document) otherSession.get( entityName, document.getId() );
								otherDoc.setSummary( "A modern classic" );
							}
					);

					try {
						document.setSummary( "A machiavellian achievement of epic proportions" );
						mainSession.flush();
						fail( "expecting opt lock failure" );
					}
					catch (PersistenceException e) {
						// expected
						checkException( mainSession, e, scope );
					}
					mainSession.clear();
				}
		);

		scope.inTransaction(
				session -> {
					Document document = (Document) session.getReference( entityName, doc.getId() );
					session.remove( document );
				}
		);
	}

	private void testDeleteOptimisticLockFailure(String entityName, SessionFactoryScope scope) {
		Document doc = scope.fromTransaction(
				session -> {
					Document document = new Document();
					document.setTitle( "Hibernate in Action" );
					document.setAuthor( "Bauer et al" );
					document.setSummary( "Very boring book about persistence" );
					document.setText( "blah blah yada yada yada" );
					document.setPubDate( new PublicationDate( 2004 ) );
					session.persist( entityName, document );
					session.flush();
					document.setSummary( "A modern classic" );
					session.flush();
					document.getPubDate().setMonth( Integer.valueOf( 3 ) );
					session.flush();
					return document;
				}
		);

		scope.inTransaction(
				mainSession -> {
					Document document = (Document) mainSession.get( entityName, doc.getId() );

					scope.inTransaction(
							otherSession -> {
								Document otherDoc = (Document) otherSession.get( entityName, document.getId() );
								otherDoc.setSummary( "my other summary" );
								otherSession.flush();
							}
					);

					try {
						mainSession.remove( document );
						mainSession.flush();
						fail( "expecting opt lock failure" );
					}
					catch (StaleObjectStateException e) {
						// expected
					}
					catch (PersistenceException e) {
						// expected
						checkException( mainSession, e, scope );
					}
					mainSession.clear();
				}
		);

		scope.inTransaction(
				session -> {
					Document document = (Document) session.getReference( entityName, doc.getId() );
					session.remove( document );
				}
		);
	}

	private void checkException(Session mainSession, PersistenceException e, SessionFactoryScope scope) {
		final Throwable cause = e.getCause();
		if ( cause instanceof JDBCException ) {
			Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
			if ( dialect instanceof SQLServerDialect && ( (JDBCException) cause ).getErrorCode() == 3960 ) {
				// SQLServer will report this condition via a SQLException
				// when using its SNAPSHOT transaction isolation.
				// it seems to "lose track" of the transaction as well...
				mainSession.getTransaction().rollback();
				mainSession.beginTransaction();
			}
			else if ( dialect instanceof CockroachDialect && ( (JDBCException) cause ).getSQLState().equals(
					"40001" ) ) {
				// CockroachDB always runs in SERIALIZABLE isolation, and uses SQL state 40001 to indicate
				// serialization failure.
			} else if (dialect instanceof MariaDBDialect && getDialect().getVersion().isAfter( 11, 6, 2 )) {
				// Mariadb snapshot_isolation throws error
			} else {
				throw e;
			}
		}
		else if ( !( cause instanceof StaleObjectStateException ) && !( cause instanceof StaleStateException ) ) {
			fail( "expected StaleObjectStateException or StaleStateException exception but is " + cause );
		}
	}

}
