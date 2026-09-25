package org.hibernate.sql.spi.mutation;

import org.hibernate.persister.entity.mutation.UpdateValuesAnalysis;

/**
 * Marker interface for analysis of new/old values.
 *
 * @see org.hibernate.engine.jdbc.mutation.MutationExecutor#execute
 * @see UpdateValuesAnalysis
 *
 * @author Steve Ebersole
 */
public interface ValuesAnalysis {
}
