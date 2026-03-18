/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.bind;

import org.hibernate.HibernateException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.TooManyRowsAffectedException;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.stat.spi.StatisticsImplementor;

import java.util.Locale;

/**
 * @author Steve Ebersole
 */
public class Checkers {

	public static boolean identifiedResultsCheck(
			Expectation expectation,
			int affectedRowCount,
			int batchPosition,
			MutationTarget<?> mutationTarget,
			TableDescriptor mutatingTable,
			Object id,
			String sqlString,
			SessionFactoryImplementor sessionFactory) {
		try {
			expectation.verifyOutcome(
					affectedRowCount,
					null,
					batchPosition,
					sqlString
			);
		}
		catch (StaleStateException e) {
			if ( !mutatingTable.isOptional() && affectedRowCount == 0 ) {
				final StatisticsImplementor statistics = sessionFactory.getStatistics();
				if ( statistics.isStatisticsEnabled() ) {
					statistics.optimisticFailure( mutationTarget.getNavigableRole().getFullPath() );
				}
				throw new StaleObjectStateException( mutationTarget.getNavigableRole().getFullPath(), id, e );
			}
			return false;
		}
		catch (TooManyRowsAffectedException e) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Duplicate identifier in table (%s) - %s#%s",
							mutatingTable.name(),
							mutationTarget.getNavigableRole().getFullPath(),
							id
					)
			);
		}
		catch (Throwable t) {
			return false;
		}

		return true;
	}
}
