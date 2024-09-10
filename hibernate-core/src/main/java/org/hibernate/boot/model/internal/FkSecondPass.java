/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;

/**
 * @author Emmanuel Bernard
 */
public abstract class FkSecondPass implements SecondPass {
	protected SimpleValue value;
	protected AnnotatedJoinColumns columns;
	/**
	 * unique counter is needed to differentiate 2 instances of FKSecondPass
	 * as they are compared.
	 * Fairly hacky but IBM VM sometimes returns the same hashCode for 2 different objects
	 * TODO is it doable to rely on the Ejb3JoinColumn names? Not sure as they could be inferred
	 */
	private final int uniqueCounter;
	private static final AtomicInteger globalCounter = new AtomicInteger();

	public FkSecondPass(SimpleValue value, AnnotatedJoinColumns columns) {
		this.value = value;
		this.columns = columns;
		this.uniqueCounter = globalCounter.getAndIncrement();
	}

	public Value getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof FkSecondPass ) ) {
			return false;
		}

		final FkSecondPass that = (FkSecondPass) o;
		return uniqueCounter == that.uniqueCounter;
	}

	@Override
	public int hashCode() {
		return uniqueCounter;
	}

	public abstract String getReferencedEntityName();

	public abstract boolean isInPrimaryKey();
}
