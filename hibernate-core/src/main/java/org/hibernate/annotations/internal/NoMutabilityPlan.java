/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations.internal;

import java.io.Serializable;

import org.hibernate.SharedSessionContract;
import org.hibernate.type.descriptor.java.MutabilityPlan;

public class NoMutabilityPlan implements MutabilityPlan<Void> {
	@Override
	public boolean isMutable() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Void deepCopy(Void value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Serializable disassemble(Void value, SharedSessionContract session) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Void assemble(Serializable cached, SharedSessionContract session) {
		throw new UnsupportedOperationException();
	}
}
