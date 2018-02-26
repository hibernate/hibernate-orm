/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.multidowncast;

import java.io.Serializable;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

/**
 * @author Christian Beikov
 */
@MappedSuperclass
public abstract class BaseEmbeddable<T extends PolymorphicBase> implements Serializable {
	private static final long serialVersionUID = 1L;

	private String someName;
	private T embeddedBase;

	public BaseEmbeddable() {
	}

	public String getSomeName() {
		return someName;
	}

	public void setSomeName(String someName) {
		this.someName = someName;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public T getEmbeddedBase() {
		return embeddedBase;
	}

	public void setEmbeddedBase(T embeddedBase) {
		this.embeddedBase = embeddedBase;
	}
}
