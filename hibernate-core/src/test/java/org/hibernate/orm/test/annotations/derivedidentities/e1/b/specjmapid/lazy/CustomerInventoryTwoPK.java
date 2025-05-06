/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.b.specjmapid.lazy;

import java.io.Serializable;

public class CustomerInventoryTwoPK implements Serializable {

private Integer id;
private int custId;

public CustomerInventoryTwoPK() {
}

public CustomerInventoryTwoPK(Integer id, int custId) {
	this.id = id;
	this.custId = custId;
}

public boolean equals(Object other) {
	if ( other == this ) {
		return true;
	}
	if ( other == null || getClass() != other.getClass() ) {
		return false;
	}
	CustomerInventoryTwoPK cip = ( CustomerInventoryTwoPK ) other;
	return ( custId == cip.custId && ( id == cip.id ||
			( id != null && id.equals( cip.id ) ) ) );
}

public int hashCode() {
	return ( id == null ? 0 : id.hashCode() ) ^ custId;
}

public Integer getId() {
	return id;
}

public int getCustId() {
	return custId;
}


}
