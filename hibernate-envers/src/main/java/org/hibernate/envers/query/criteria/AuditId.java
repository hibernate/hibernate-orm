/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.criteria;

import org.hibernate.envers.query.criteria.internal.IdentifierEqAuditExpression;
import org.hibernate.envers.query.internal.property.EntityPropertyName;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

/**
 * Create restrictions and projections for the id of an audited entity.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@SuppressWarnings({"JavaDoc"})
public class AuditId<T> extends AuditProperty<T> {
	public static final String IDENTIFIER_PLACEHOLDER = "$$id$$";
	private static final PropertyNameGetter IDENTIFIER_PROPERTY_GETTER = new EntityPropertyName( IDENTIFIER_PLACEHOLDER );

	private final String alias;

	public AuditId(String alias) {
		super( alias, IDENTIFIER_PROPERTY_GETTER );
		this.alias = alias;
	}

	/**
	 * Apply an "equal" constraint
	 */
	@Override
	public AuditCriterion eq(Object id) {
		return new IdentifierEqAuditExpression( alias, id, true );
	}

	/**
	 * Apply a "not equal" constraint
	 */
	@Override
	public AuditCriterion ne(Object id) {
		return new IdentifierEqAuditExpression( alias, id, false );
	}

	// Projections

	@Override
	public AuditCriterion hasChanged() {
		throw new UnsupportedOperationException();
	}

	@Override
	public AuditCriterion hasNotChanged() {
		throw new UnsupportedOperationException();
	}
}
