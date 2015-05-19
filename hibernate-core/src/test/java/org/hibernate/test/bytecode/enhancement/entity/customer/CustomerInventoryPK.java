/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.entity.customer;

import java.io.Serializable;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CustomerInventoryPK implements Serializable {

    private Long id;
    private int custId;

    public CustomerInventoryPK() {
    }

    public CustomerInventoryPK(Long id, int custId) {
        this.id = id;
        this.custId = custId;
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        CustomerInventoryPK cip = (CustomerInventoryPK) other;
        return (custId == cip.custId && (id == cip.id ||
                ( id != null && id.equals(cip.id))));
    }

    public int hashCode() {
        return (id == null ? 0 : id.hashCode()) ^ custId;
    }

    public Long getId() {
        return id;
    }

    public int getCustId() {
        return custId;
    }
}
