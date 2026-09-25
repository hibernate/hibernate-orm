package org.hibernate.action.queue.spi.bind;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.TooManyRowsAffectedException;
import org.hibernate.sql.spi.mutation.MutationTarget;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

/// @author Steve Ebersole
/// @since 8.0
@Incubating(since = "8.0", group = "action-queue")
public class Checkers {

	public static boolean identifiedResultsCheck(
			Expectation expectation,
			int affectedRowCount,
			PreparedStatement statement,
			int batchPosition,
			MutationTarget mutationTarget,
			TableDescriptor mutatingTable,
			Object id,
			String sqlString,
			SessionFactoryImplementor sessionFactory) throws SQLException {
		try {
			expectation.verifyOutcome(
					affectedRowCount,
					statement,
					batchPosition,
					sqlString
			);
		}
		catch (StaleStateException e) {
			// For an OUT parameter, the JDBC update count is not the checked row count.
			if ( !mutatingTable.isOptional()
					&& (affectedRowCount == 0 || expectation instanceof Expectation.OutParameter) ) {
				final var statistics = sessionFactory.getStatistics();
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

		return true;
	}
}
