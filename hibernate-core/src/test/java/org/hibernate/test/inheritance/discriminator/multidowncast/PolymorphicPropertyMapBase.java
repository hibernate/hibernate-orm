/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.multidowncast;

import javax.persistence.Embedded;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

/**
 * @param <T>
 *
 * @author Christian Beikov
 */
@MappedSuperclass
public abstract class PolymorphicPropertyMapBase<T extends PolymorphicBase, E extends BaseEmbeddable> extends PolymorphicPropertyBase {

	private static final long serialVersionUID = 1L;

	private T base;
	private E baseEmbeddable;

	public PolymorphicPropertyMapBase() {
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public T getBase() {
		return base;
	}

	public void setBase(T base) {
		this.base = base;
	}

	@Embedded
	public E getBaseEmbeddable() {
		return baseEmbeddable;
	}

	public void setBaseEmbeddable(E baseEmbeddable) {
		this.baseEmbeddable = baseEmbeddable;
	}
}
