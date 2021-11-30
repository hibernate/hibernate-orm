/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.temptable;

import java.util.function.Function;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public interface TemporaryTableExporter {
	String getSqlCreateCommand(TemporaryTable idTable);

	String getSqlDropCommand(TemporaryTable idTable);

	String getSqlTruncateCommand(
			TemporaryTable idTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SharedSessionContractImplementor session);
}
