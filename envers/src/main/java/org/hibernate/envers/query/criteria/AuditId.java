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
 */

package org.hibernate.envers.query.criteria;

import org.hibernate.envers.query.projection.AuditProjection;
import org.hibernate.envers.query.projection.PropertyAuditProjection;
import org.hibernate.envers.query.property.OriginalIdPropertyName;

/**
 * Create restrictions and projections for the id of an audited entity.
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"JavaDoc"})
public class AuditId {
    /**
	 * Apply an "equal" constraint
	 */
	public AuditCriterion eq(Object id) {
		return new IdentifierEqAuditExpression(id, true);
	}

    /**
	 * Apply a "not equal" constraint
	 */
	public AuditCriterion ne(Object id) {
		return new IdentifierEqAuditExpression(id, false);
	}

    // Projections

    /**
     * Projection counting the values
     * TODO: idPropertyName isn't needed, should be read from the configuration
     * @param idPropertyName Name of the identifier property
     */
    public AuditProjection count(String idPropertyName) {
        return new PropertyAuditProjection(new OriginalIdPropertyName(idPropertyName), "count", false);
    }
}