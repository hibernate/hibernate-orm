/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.group;

import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;

/**
 * Indicates an attempt to access the parameter for an unknown column
 *
 * @see org.hibernate.sql.model.MutationOperation#getJdbcValueDescriptor(String, ParameterUsage)
 *
 * @author Steve Ebersole
 */
public class UnknownParameterException extends HibernateException {
	private final MutationType mutationType;
	private final MutationTarget<?> mutationTarget;
	private final String tableName;
	private final String columnName;
	private final ParameterUsage usage;

	public UnknownParameterException(
			MutationType mutationType,
			MutationTarget<?> mutationTarget,
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
		this.mutationType = mutationType;
		this.mutationTarget = mutationTarget;
		this.tableName = tableName;
		this.columnName = columnName;
		this.usage = usage;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"UnknownParameterException(`%s.%s` for %s - %s : %s)",
				tableName,
				columnName,
				usage,
				mutationType.name(),
				StringHelper.collapse( mutationTarget.getRolePath() )
		);
	}
}
