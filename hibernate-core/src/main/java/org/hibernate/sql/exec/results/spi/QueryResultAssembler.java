/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.spi;

import java.sql.SQLException;

/**
 * Responsible for "assembling" a result for inclusion in the domain query result.
 *
 * @author Steve Ebersole
 */
public interface QueryResultAssembler {
	// todo (6.0) : ? - (like on QueryResult) JavaTypeDescriptor or ExpressableType instead of Java type?
	// todo (6.0) : is this even needed in any form?
	Class getReturnedJavaType();
	Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) throws SQLException;
}
