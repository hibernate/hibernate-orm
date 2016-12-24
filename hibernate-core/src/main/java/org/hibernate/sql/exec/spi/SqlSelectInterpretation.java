/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.spi;

import java.util.List;

import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.spi.ParameterBinder;

/**
 * Represents the {@link SqlAstSelectInterpreter}'s interpretation of a select query
 *
 * @author Steve Ebersole
 */
public interface SqlSelectInterpretation {
	// todo : split out a SqlInterpretation that can be used for INSERT/UPDATE/DELETE

	String getSql();
	List<ParameterBinder> getParameterBinders();

	// todo : add "parameter extractors" for procedure/function calls?

	// todo : have this subclass SqlInterpretation adding Returns

	List<Return> getReturns();

}
