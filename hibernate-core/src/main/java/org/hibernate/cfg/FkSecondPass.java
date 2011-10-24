/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;

/**
 * @author Emmanuel Bernard
 */
public abstract class FkSecondPass implements SecondPass {
	protected SimpleValue value;
	protected Ejb3JoinColumn[] columns;
	/**
	 * unique counter is needed to differentiate 2 instances of FKSecondPass
	 * as they are compared.
	 * Fairly hacky but IBM VM sometimes returns the same hashCode for 2 different objects
	 * TODO is it doable to rely on the Ejb3JoinColumn names? Not sure at they could be inferred
	 */
	private int uniqueCounter;
	private static AtomicInteger globalCounter = new AtomicInteger();

	public FkSecondPass(SimpleValue value, Ejb3JoinColumn[] columns) {
		this.value = value;
		this.columns = columns;
		this.uniqueCounter = globalCounter.getAndIncrement();
	}

	public int getUniqueCounter() {
		return uniqueCounter;
	}

	public Value getValue() {
		return value;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof FkSecondPass ) ) return false;

		FkSecondPass that = (FkSecondPass) o;

		if ( uniqueCounter != that.uniqueCounter ) return false;

		return true;
	}

	public int hashCode() {
		return uniqueCounter;
	}

	public abstract String getReferencedEntityName();

	public abstract boolean isInPrimaryKey();
}
