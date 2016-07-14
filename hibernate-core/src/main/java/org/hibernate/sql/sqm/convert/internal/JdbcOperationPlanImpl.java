/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.convert.internal;

import java.util.List;

import org.hibernate.sql.gen.JdbcOperationPlan;
import org.hibernate.sql.gen.NotYetImplementedException;
import org.hibernate.sql.gen.ParameterBinder;
import org.hibernate.sql.gen.QueryOptionBinder;

/**
 *
 * @author Steve Ebersole
 * @author John O'Hara
 */
public class JdbcOperationPlanImpl implements JdbcOperationPlan {
	@Override
	public String getSql() {
		throw new NotYetImplementedException();
	}

	@Override
	public List<ParameterBinder> getParameterBinders() {
		throw new NotYetImplementedException();
	}

	@Override
	public List<QueryOptionBinder> getQueryOptionBinders() {
		throw new NotYetImplementedException();
	}
}
