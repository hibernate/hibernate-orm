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
package org.hibernate.envers.test.integration.naming.ids;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Embeddable
public class EmbIdNaming implements Serializable {
    @Column(name = "XX")
    private Integer x;

    @Column(name = "YY")
    private Integer y;

    public EmbIdNaming() {
    }

    public EmbIdNaming(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmbIdNaming)) return false;

        EmbIdNaming embId = (EmbIdNaming) o;

        if (x != null ? !x.equals(embId.x) : embId.x != null) return false;
        if (y != null ? !y.equals(embId.y) : embId.y != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (x != null ? x.hashCode() : 0);
        result = 31 * result + (y != null ? y.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "EmbIdNaming(" + x + ", " + y + ")";
    }
}