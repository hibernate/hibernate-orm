/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.mixed.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import org.hibernate.envers.Audited;

@Audited
@Entity
@DiscriminatorValue(value = "NORMAL")
public class NormalActivity extends AbstractActivity {

	@Override
	public boolean equals(Object obj) {
		if ( obj == this ) {
			return true;
		}
		if ( !(obj instanceof NormalActivity) ) {
			return false;
		}
		NormalActivity normalActivity = (NormalActivity) obj;
		return getId().equals( normalActivity.getId() );
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}
}
