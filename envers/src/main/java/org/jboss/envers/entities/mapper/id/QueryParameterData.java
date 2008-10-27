/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.entities.mapper.id;

import org.hibernate.Query;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class QueryParameterData {
    private String flatEntityPropertyName;
    private Object value;

    public QueryParameterData(String flatEntityPropertyName, Object value) {
        this.flatEntityPropertyName = flatEntityPropertyName;
        this.value = value;
    }

    public String getProperty(String prefix) {
        if (prefix != null) {
            return prefix + "." + flatEntityPropertyName;
        } else {
            return flatEntityPropertyName;
        }
    }

    public Object getValue() {
        return value;
    }

    public void setParameterValue(Query query) {
        query.setParameter(flatEntityPropertyName, value);
    }

    public String getQueryParameterName() {
        return flatEntityPropertyName;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QueryParameterData)) return false;

        QueryParameterData that = (QueryParameterData) o;

        if (flatEntityPropertyName != null ? !flatEntityPropertyName.equals(that.flatEntityPropertyName) : that.flatEntityPropertyName != null)
            return false;

        return true;
    }

    public int hashCode() {
        return (flatEntityPropertyName != null ? flatEntityPropertyName.hashCode() : 0);
    }
}
