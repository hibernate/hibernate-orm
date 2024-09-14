/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhance.internal.bytebuddy;

import java.io.Serializable;

import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;

// This class must not be nested in the test class, otherwise its private fields will be visible
// from subclasses and we won't reproduce the bug.
@MappedSuperclass
public abstract class MyNonVisibleGenericExtendsSerializableMappedSuperclass<C extends Serializable> {

	@Embedded
	private C embedded;

	public C getEmbedded() {
		return embedded;
	}

	public void setEmbedded(C embedded) {
		this.embedded = embedded;
	}
}
