/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.test.annotations.derivedidentities.e1.b.embeddedidcount;

import java.io.Serializable;

@SuppressWarnings("serial")
public class PurchaseOrderPK implements Serializable {
    private int id;
    private Integer site;

    public PurchaseOrderPK() {
    }

    public PurchaseOrderPK(Integer site, int id) {
        this.site = site;
        this.id = id;  
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        PurchaseOrderPK pop = (PurchaseOrderPK) other;
        return (id == pop.id && (site == pop.site || 
                        (site != null && site.equals(pop.site))));
    }

    public int hashCode() {
        return (site == null ? 0 : site.hashCode()) ^ id;
    }

    public int getId() {
        return id;
    }

    public Integer getSite() {
        return site;
    }
}
