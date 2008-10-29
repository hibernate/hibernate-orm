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
package org.hibernate.envers.test.integration.data;

import java.io.Serializable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class SerObject implements Serializable {
    static final long serialVersionUID = 982352321924L;

    private String data;

    public SerObject() {
    }

    public SerObject(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SerObject)) return false;

        SerObject serObject = (SerObject) o;

        if (data != null ? !data.equals(serObject.data) : serObject.data != null) return false;

        return true;
    }

    public int hashCode() {
        return (data != null ? data.hashCode() : 0);
    }
}
