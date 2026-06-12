/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.complete;

import java.io.Serializable;

import org.hibernate.SharedSessionContract;
import org.hibernate.type.descriptor.java.MutabilityPlan;

public class XmlSourceParityMutabilityPlan implements MutabilityPlan<String> {
	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public String deepCopy(String value) {
		return value;
	}

	@Override
	public Serializable disassemble(String value, SharedSessionContract session) {
		return value;
	}

	@Override
	public String assemble(Serializable cached, SharedSessionContract session) {
		return (String) cached;
	}
}
