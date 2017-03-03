/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.multidowncast;

import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

/**
 * @author Christian Beikov
 */
@Embeddable
public abstract class Embeddable1 extends BaseEmbeddable<PolymorphicSub1> {
	private static final long serialVersionUID = 1L;

	private String someName1;
	private PolymorphicSub1 embeddedRelation1;

	public Embeddable1() {
	}

	public String getSomeName1() {
		return someName1;
	}

	public void setSomeName1(String someName1) {
		this.someName1 = someName1;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public PolymorphicSub1 getEmbeddedRelation1() {
		return embeddedRelation1;
	}

	public void setEmbeddedRelation1(PolymorphicSub1 embeddedRelation1) {
		this.embeddedRelation1 = embeddedRelation1;
	}
}
