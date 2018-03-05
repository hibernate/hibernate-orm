/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.classic;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.hql.spi.NamedParameterInformation;

/**
 * @author Steve Ebersole
 */
public class NamedParameterInformationImpl extends AbstractParameterInformation implements NamedParameterInformation {
	private final String name;

	NamedParameterInformationImpl(String name) {
		this.name = name;
	}

	@Override
	public String getSourceName() {
		return name;
	}

	@Override
	public int bind(
			PreparedStatement statement,
			QueryParameters qp,
			SharedSessionContractImplementor session,
			int position) throws SQLException {
		final TypedValue typedValue = qp.getNamedParameters().get( name );
		typedValue.getType().nullSafeSet( statement, typedValue.getValue(), position, session );
		return typedValue.getType().getColumnSpan( session.getFactory() );
	}
}
