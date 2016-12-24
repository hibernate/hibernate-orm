/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.internal;

import java.util.List;

import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.exec.spi.SqlSelectInterpretation;
import org.hibernate.sql.spi.ParameterBinder;

/**
 * @author Steve Ebersole
 */
public class SqlSelectInterpretationImpl implements SqlSelectInterpretation {
	private final String sql;
	private final List<ParameterBinder> parameterBinders;
	private final List<Return> returns;

	public SqlSelectInterpretationImpl(String sql, List<ParameterBinder> parameterBinders, List<Return> returns) {
		this.sql = sql;
		this.parameterBinders = parameterBinders;
		this.returns = returns;
	}

	@Override
	public String getSql() {
		return sql;
	}

	@Override
	public List<ParameterBinder> getParameterBinders() {
		return parameterBinders;
	}

	@Override
	public List<Return> getReturns() {
		return returns;
	}
}
