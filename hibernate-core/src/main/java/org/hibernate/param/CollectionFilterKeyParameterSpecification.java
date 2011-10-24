/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.param;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.Type;

/**
 * A specialized ParameterSpecification impl for dealing with a collection-key as part of a collection filter
 * compilation.
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
	 * @param queryParameterPosition The position within {@link org.hibernate.engine.spi.QueryParameters} where
	 * we can find the appropriate param value to bind.
	 */
	public CollectionFilterKeyParameterSpecification(String collectionRole, Type keyType, int queryParameterPosition) {
		this.collectionRole = collectionRole;
		this.keyType = keyType;
		this.queryParameterPosition = queryParameterPosition;
	}

	/**
	 * {@inheritDoc}
	 */
	public int bind(
			PreparedStatement statement,
			QueryParameters qp,
			SessionImplementor session,
			int position) throws SQLException {
		Object value = qp.getPositionalParameterValues()[queryParameterPosition];
		keyType.nullSafeSet( statement, value, position, session );
		return keyType.getColumnSpan( session.getFactory() );
	}

	/**
	 * {@inheritDoc}
	 */
	public Type getExpectedType() {
		return keyType;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setExpectedType(Type expectedType) {
		// todo : throw exception?
	}

	/**
	 * {@inheritDoc}
	 */
	public String renderDisplayInfo() {
		return "collection-filter-key=" + collectionRole;
	}
}
