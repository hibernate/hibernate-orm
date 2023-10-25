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
package org.hibernate.id.enhanced;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.id.SequenceValueExtractor;
import org.hibernate.internal.SessionImpl;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HHH-14656")
public abstract class AbstractLegacyHiLoAlgorithmOptimizerIdTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testSequenceIdentifierGenerator() {
		try (final Session s = openSession()) {
			final SequenceValueExtractor sequenceValueExtractor = new SequenceValueExtractor(
					getDialect(),
					SequenceIdentifier.SEQUENCE_NAME
			);

			{
				final Transaction tx = s.beginTransaction();

				try {
					for ( int i = 0; i < SequenceIdentifier.ALLOCATION_SIZE; i++ ) {
						s.persist( new SequenceIdentifier() );
					}

					tx.commit();
				}
				catch (RuntimeException e) {
					tx.rollback();
				}

				assertEquals( SequenceIdentifier.ALLOCATION_SIZE, countInsertedRows( s ) );
				assertEquals( 1L, sequenceValueExtractor.extractSequenceValue( (SessionImpl) s ) );
			}

			{
				final Transaction tx = s.beginTransaction();

				try {
					s.persist( new SequenceIdentifier() );
					tx.commit();
				}
				catch (RuntimeException e) {
					tx.rollback();
				}

				assertEquals( SequenceIdentifier.ALLOCATION_SIZE + 1, countInsertedRows( s ) );
				assertEquals( 2L, sequenceValueExtractor.extractSequenceValue( (SessionImpl) s ) );
			}
		}
	}

	private int countInsertedRows(Session s) {
		return ( (Number) s.createSQLQuery( "SELECT COUNT(*) FROM sequenceIdentifier" )
				.uniqueResult() ).intValue();
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected Class[] getAnnotatedClasses() {
		return new Class[] { SequenceIdentifier.class };
	}

	@Entity(name = "sequenceIdentifier")
	public static class SequenceIdentifier {
		private static final int ALLOCATION_SIZE = 50;
		private static final String SEQUENCE_NAME = "test_sequence";

		@Id
		@SequenceGenerator(name = "sampleGenerator", sequenceName = SEQUENCE_NAME, allocationSize = ALLOCATION_SIZE)
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sampleGenerator")
		private long id;
	}
}

