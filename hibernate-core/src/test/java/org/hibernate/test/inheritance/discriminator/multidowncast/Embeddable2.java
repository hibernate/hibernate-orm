/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.multidowncast;

import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

/**
 * @author Christian Beikov
 */
@Embeddable
public abstract class Embeddable2 extends BaseEmbeddable<PolymorphicSub2> {
	private static final long serialVersionUID = 1L;

	private String someName2;
	private PolymorphicSub2 embeddedRelation2;

	public Embeddable2() {
	}

	public String getSomeName2() {
		return someName2;
	}

	public void setSomeName2(String someName2) {
		this.someName2 = someName2;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public PolymorphicSub2 getEmbeddedRelation2() {
		return embeddedRelation2;
	}

	public void setEmbeddedRelation2(PolymorphicSub2 embeddedRelation2) {
		this.embeddedRelation2 = embeddedRelation2;
	}
}
