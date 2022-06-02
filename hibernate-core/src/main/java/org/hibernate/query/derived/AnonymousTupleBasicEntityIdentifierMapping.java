/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.derived;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.SqmExpressible;

/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleBasicEntityIdentifierMapping extends AnonymousTupleBasicValuedModelPart
		implements BasicEntityIdentifierMapping {

	private final BasicEntityIdentifierMapping delegate;

	public AnonymousTupleBasicEntityIdentifierMapping(
			String selectionExpression,
			SqmExpressible<?> expressible,
			JdbcMapping jdbcMapping,
			BasicEntityIdentifierMapping delegate) {
		super( delegate.getAttributeName(), selectionExpression, expressible, jdbcMapping );
		this.delegate = delegate;
	}

	@Override
	public IdentifierValue getUnsavedStrategy() {
		return delegate.getUnsavedStrategy();
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		return delegate.getIdentifier( entity, session );
	}

	@Override
	public Object getIdentifier(Object entity) {
		return delegate.getIdentifier( entity );
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		delegate.setIdentifier( entity, id, session );
	}

	@Override
	public Object instantiate() {
		return delegate.instantiate();
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return delegate.getPropertyAccess();
	}

	@Override
	public String getAttributeName() {
		return getPartName();
	}
}
