/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.query.order.internal;

import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;
import org.hibernate.envers.query.order.AuditOrder;
import org.hibernate.envers.query.order.NullPrecedence;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class PropertyAuditOrder implements AuditOrder {
	private final String alias;
	private final PropertyNameGetter propertyNameGetter;
	private final boolean asc;
	private NullPrecedence nullPrecedence;

	public PropertyAuditOrder(String alias, PropertyNameGetter propertyNameGetter, boolean asc) {
		this.alias = alias;
		this.propertyNameGetter = propertyNameGetter;
		this.asc = asc;
	}

	@Override
	public AuditOrder nulls(NullPrecedence nullPrecedence) {
		this.nullPrecedence = nullPrecedence;
		return this;
	}

	@Override
	public OrderData getData(Configuration configuration) {
		return new OrderData( alias, propertyNameGetter.get( configuration ), asc, nullPrecedence );
	}
}
