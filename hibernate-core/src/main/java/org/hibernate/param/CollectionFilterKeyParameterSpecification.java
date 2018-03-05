/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.param;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.Type;

/**
 * A specialized ParameterSpecification impl for dealing with a collection-key as part of a collection filter
 * compilation.
 *
 * @author Steve Ebersole
 */
public class CollectionFilterKeyParameterSpecification implements ParameterSpecification {
	public static final String PARAM_KEY = "{collection_key}";

	private final String collectionRole;
	private final Type keyType;

	/**
	 * Creates a specialized collection-filter collection-key parameter spec.
	 *
	 * @param collectionRole The collection role being filtered.
	 * @param keyType The mapped collection-key type.
	 */
	public CollectionFilterKeyParameterSpecification(String collectionRole, Type keyType) {
		this.collectionRole = collectionRole;
		this.keyType = keyType;
	}

	@Override
	public int bind(
			PreparedStatement statement,
			QueryParameters qp,
			SharedSessionContractImplementor session,
			int position) throws SQLException {
		final Object value = qp.getNamedParameters().get( PARAM_KEY ).getValue();
		keyType.nullSafeSet( statement, value, position, session );
		return keyType.getColumnSpan( session.getFactory() );
	}

	@Override
	public Type getExpectedType() {
		return keyType;
	}

	@Override
	public void setExpectedType(Type expectedType) {
		// todo : throw exception?
	}

	@Override
	public String renderDisplayInfo() {
		return "collection-filter-key=" + collectionRole;
	}
}
