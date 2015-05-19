/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.entity.customer;

import javax.persistence.Embeddable;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Embeddable
public class SupplierComponentPK {

    String componentID;
    int supplierID;

    public SupplierComponentPK() {
    }

    public SupplierComponentPK(String suppCompID, int suppCompSuppID) {
        this.componentID = suppCompID;
        this.supplierID = suppCompSuppID;
    }

    public String getComponentID() {
        return componentID;
    }

    public int getSupplierID() {
        return supplierID;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + componentID.hashCode();
        result = PRIME * result + supplierID;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final SupplierComponentPK other = (SupplierComponentPK) obj;
        return componentID.equals(other.componentID);
    }
}
