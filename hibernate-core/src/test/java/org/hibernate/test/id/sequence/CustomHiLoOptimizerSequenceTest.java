/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.sequence;

import java.io.Serializable;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.id.enhanced.AccessCallback;
import org.hibernate.id.enhanced.HiLoOptimizer;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-13865")
public class CustomHiLoOptimizerSequenceTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testCustomSequence() {

		final Statistics statistics = sessionFactory().getStatistics();

		statistics.clear();
		doInHibernate( this::sessionFactory, session -> {
			for ( int i = 0 ; i < 7 ; i++ ) {
				session.persist( new AnEntity( "Entity #" + i ) );
				if ( i == 0 || i == 3 || i == 6 ) {
					assertEquals( 1, statistics.getPrepareStatementCount() );
					statistics.clear();
				}
				else {
					assertEquals( 0, statistics.getPrepareStatementCount() );
				}
			}
		});

		// CustomHiLoOptimizer sets the nextValue to 10 * the value returned by the sequence.
		// Since the sequence for AnEntity maps initialValue = 2 and allocationSize = 3, that means:
		// - Entity with string property, "Entity #0", should have id set to 20 * 3 - 3  + 1 = 58;
		// - Entity with string property, "Entity #1", should have id set to 59;
		// - Entity with string property, "Entity #2", should have id set to 60;
		// - Entity with string property, "Entity #3", should have id set to 30 * 3 - 3 + 1 = 88;
		// - Entity with string property, "Entity #4", should have id set to 89;
		// - Entity with string property, "Entity #5", should have id set to 90;
		// - Entity with string property, "Entity #6", should have id set to 40 * 3 - 3 + 1 = 118;

		doInHibernate( this::sessionFactory, session -> {
			assertEquals( "Entity #0", session.get( AnEntity.class, 58 ).string );
			assertEquals( "Entity #1", session.get( AnEntity.class, 59 ).string );
			assertEquals( "Entity #2", session.get( AnEntity.class, 60 ).string );
			assertEquals( "Entity #3", session.get( AnEntity.class, 88 ).string );
			assertEquals( "Entity #4", session.get( AnEntity.class, 89 ).string );
			assertEquals( "Entity #5", session.get( AnEntity.class, 90 ).string );
			assertEquals( "Entity #6", session.get( AnEntity.class, 118 ).string );
		});
	}

	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.PREFERRED_POOLED_OPTIMIZER, CustomHiLoOptimizer.class.getName() );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { AnEntity.class };
	}

	@Entity(name = "AnEntity")
	@SequenceGenerator(name = "seq",
			sequenceName = "hilo_id_seq",
			initialValue = 2,
			allocationSize = 3
	)
	public static class AnEntity {
		@Id
		@GeneratedValue(generator = "seq")
		Integer id;

		String string;

		public AnEntity() {
		}

		public AnEntity(String string) {
			this.string = string;
		}
	}

	public static class CustomHiLoOptimizer extends HiLoOptimizer {

		public CustomHiLoOptimizer(Class returnClass, int incrementSize) {
			super( returnClass, incrementSize );
		}

		@Override
		public synchronized Serializable generate(AccessCallback callback) {
			final GenerationState generationState = locateGenerationState( callback.getTenantIdentifier() );

			if ( !generationState.isInitialized() ) {
				// first call, so initialize ourselves.  we need to read the database
				// value and set up the 'bucket' boundaries

				IntegralDataTypeHolder firstSourceValue = getNextValue( callback );
				while ( firstSourceValue.lt( 1 ) ) {
					firstSourceValue = getNextValue( callback );
				}
				generationState.updateFromNewSourceValue( firstSourceValue );
			}
			else if ( generationState.requiresNewSourceValue() ) {
				generationState.updateFromNewSourceValue( getNextValue( callback ) );
			}
			return generationState.makeValueThenIncrement();
		}

		private IntegralDataTypeHolder getNextValue(AccessCallback callback) {
			return callback.getNextValue().multiplyBy( 10 );
		}
	}
}
