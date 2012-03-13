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

import javax.persistence.Embeddable;
import java.io.Serializable;

@SuppressWarnings("serial")
@Embeddable
public class PurchaseOrderLinePK implements Serializable {
    int lineNumber;
    int poID;
    int location;

    public PurchaseOrderLinePK() {
    }

    public PurchaseOrderLinePK(int poLineNumber, int poLinePoID, int location) {
        this.lineNumber = poLineNumber;
        this.poID   = poLinePoID;
        this.location = location;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getPoID() {
        return poID;
    }

    public int getLocation() {
        return location;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + lineNumber;
        result = PRIME * result + poID;
        result = PRIME * result + location;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final PurchaseOrderLinePK other = (PurchaseOrderLinePK) obj;
        if (lineNumber != other.lineNumber)
            return false;
        if (poID != other.poID)
            return false;
        if (location != other.location)
            return false;
        return true;
    }
}
