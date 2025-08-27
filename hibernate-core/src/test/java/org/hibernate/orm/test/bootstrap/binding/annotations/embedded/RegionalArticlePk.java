/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;
import java.io.Serializable;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;

import jakarta.persistence.Access;

/**
 * Regional article pk
 *
 * @author Emmanuel Bernard
 */
@Embeddable
@Access(AccessType.FIELD)
public class RegionalArticlePk implements Serializable {
	/**
	 * country iso2 code
	 */
	public String iso2;
	public String localUniqueKey;

	public int hashCode() {
		//this implem sucks
		return ( iso2 + localUniqueKey ).hashCode();
	}

	public boolean equals(Object obj) {
		//iso2 and localUniqueKey are expected to be set in this implem
		if ( obj != null && obj instanceof RegionalArticlePk ) {
			RegionalArticlePk other = (RegionalArticlePk) obj;
			return iso2.equals( other.iso2 ) && localUniqueKey.equals( other.localUniqueKey );
		}
		else {
			return false;
		}
	}
}
