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
import org.hibernate.hql.spi.PositionalParameterInformation;

/**
 * @author Steve Ebersole
 */
public class PositionalParameterInformationImpl
		extends AbstractParameterInformation
		implements PositionalParameterInformation {
	private final int label;

	public PositionalParameterInformationImpl(int label) {
		this.label = label;
	}

	@Override
	public int getLabel() {
		return label;
	}

	@Override
	public int bind(
			PreparedStatement statement,
			QueryParameters qp,
			SharedSessionContractImplementor session,
			int position) throws SQLException {
		final TypedValue typedValue = qp.getNamedParameters().get( Integer.toString( label ) );
		typedValue.getType().nullSafeSet( statement, typedValue.getValue(), position, session );
		return typedValue.getType().getColumnSpan( session.getFactory() );
	}
}
