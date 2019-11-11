/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.util.function.Function;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public interface IdTableExporter {
	String getSqlCreateCommand(IdTable idTable);

	String getSqlDropCommand(IdTable idTable);

	String getSqlTruncateCommand(
			IdTable idTable,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SharedSessionContractImplementor session);
}
