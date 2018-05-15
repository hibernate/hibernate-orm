/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.Locale;

import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.sqm.tree.SqmNonSelectStatement;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmStatement;

/**
 * Helper utilities for dealing with SQM
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class SqmUtil {
	private SqmUtil() {
	}

	public static void verifyIsSelectStatement(SqmStatement sqm) {
		if ( !SqmSelectStatement.class.isInstance( sqm ) ) {
			throw new IllegalQueryOperationException(
					String.format(
							Locale.ROOT,
							"Expecting a SELECT Query [%s], but found %s",
							SqmSelectStatement.class.getName(),
							sqm.getClass().getName()
					)
			);
		}
	}

	public static void verifyIsNonSelectStatement(SqmStatement sqm) {
		if ( !SqmNonSelectStatement.class.isInstance( sqm ) ) {
			throw new IllegalQueryOperationException(
					String.format(
							Locale.ROOT,
							"Expecting a non-SELECT Query [%s], but found %s",
							SqmNonSelectStatement.class.getName(),
							sqm.getClass().getName()
					)
			);
		}
	}
}
