/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.hibernate.test.bytecode.enhancement.customer;

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
