package org.hibernate.engine.jdbc.mutation.group;

import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.MutationType;

/**
 * Indicates an attempt to access the parameter for an unknown column
 *
 * @see org.hibernate.sql.spi.mutation.MutationOperation#getJdbcValueDescriptor(String, ParameterUsage)
 *
 * @author Steve Ebersole
 */
public class UnknownParameterException extends HibernateException {

	public UnknownParameterException(
			MutationType mutationType,
			MutationTarget mutationTarget,
			String tableName,
			String columnName,
			ParameterUsage usage) {
		super( String.format(
				Locale.ROOT,
				"Unable to locate parameter `%s.%s` for %s - %s : %s",
				tableName,
				columnName,
				usage,
				mutationType.name(),
				mutationTarget.getRolePath()
		) );
	}

	public UnknownParameterException(
			MutationType mutationType,
			String mutationTargetRole,
			String tableName,
			String columnName,
			ParameterUsage usage) {
		super( String.format(
				Locale.ROOT,
				"Unable to locate parameter `%s.%s` for %s - %s : %s",
				tableName,
				columnName,
				usage,
				mutationType.name(),
				mutationTargetRole
		) );
	}
}
