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

package org.hibernate.envers.query.projection;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.tools.Triple;
import org.hibernate.envers.query.property.PropertyNameGetter;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class PropertyAuditProjection implements AuditProjection {
    private final PropertyNameGetter propertyNameGetter;
    private final String function;
    private final boolean distinct;

    public PropertyAuditProjection(PropertyNameGetter propertyNameGetter, String function, boolean distinct) {
        this.propertyNameGetter = propertyNameGetter;
        this.function = function;
        this.distinct = distinct;
    }

    public Triple<String, String, Boolean> getData(AuditConfiguration auditCfg) {
        String propertyName = propertyNameGetter.get(auditCfg);

        return Triple.make(function, propertyName, distinct);
    }
}