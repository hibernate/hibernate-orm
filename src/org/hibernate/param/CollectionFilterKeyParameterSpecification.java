package org.hibernate.param;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;

/**
 * A specialized ParameterSpecification impl for dealing with a collection-key
 * as part of a collection filter compilation.
 *
 * @author Steve Ebersole
 */
public class CollectionFilterKeyParameterSpecification implements ParameterSpecification {

	private final String collectionRole;
	private final Type keyType;
	private final int queryParameterPosition;


	/**
	 * Creates a specialized collection-filter collection-key parameter spec.
	 *
	 * @param collectionRole The collection role being filtered.
	 * @param keyType The mapped collection-key type.
	 * @param queryParameterPosition The position within {@link org.hibernate.engine.QueryParameters} where
	 * we can find the appropriate param value to bind.
	 */
	public CollectionFilterKeyParameterSpecification(String collectionRole, Type keyType, int queryParameterPosition) {
		this.collectionRole = collectionRole;
		this.keyType = keyType;
		this.queryParameterPosition = queryParameterPosition;
	}

	public int bind(
			PreparedStatement statement,
			QueryParameters qp,
			SessionImplementor session,
			int position) throws SQLException {
		Object value = qp.getPositionalParameterValues()[queryParameterPosition];
		keyType.nullSafeSet( statement, value, position, session );
		return keyType.getColumnSpan( session.getFactory() );
	}

	public Type getExpectedType() {
		return keyType;
	}

	public void setExpectedType(Type expectedType) {
		// todo : throw exception?
	}

	public String renderDisplayInfo() {
		return "collection-filter-key=" + collectionRole;
	}
}
