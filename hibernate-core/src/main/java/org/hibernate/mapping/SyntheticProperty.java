/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;


/**
 * Models a property which does not actually exist in the model.  It is created by Hibernate during
 * the metamodel binding process.
 *
 * @author Steve Ebersole
 */
public class SyntheticProperty extends Property {
	@Override
	public boolean isSynthetic() {
		return true;
	}
}
