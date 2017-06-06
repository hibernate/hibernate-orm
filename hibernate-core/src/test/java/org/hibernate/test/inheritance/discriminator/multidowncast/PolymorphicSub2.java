/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.multidowncast;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

/**
 * @author Christian Beikov
 */
@Entity
public class PolymorphicSub2 extends PolymorphicBase {
	private static final long serialVersionUID = 1L;

	private IntIdEntity relation2;
	private PolymorphicBase parent2;
	private NameObject embeddable2;
	private Integer sub2Value;

	public PolymorphicSub2() {
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public IntIdEntity getRelation2() {
		return relation2;
	}

	public void setRelation2(IntIdEntity relation2) {
		this.relation2 = relation2;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public PolymorphicBase getParent2() {
		return parent2;
	}

	public void setParent2(PolymorphicBase parent1) {
		this.parent2 = parent1;
	}

	@Embedded
	public NameObject getEmbeddable2() {
		return embeddable2;
	}

	public void setEmbeddable2(NameObject embeddable1) {
		this.embeddable2 = embeddable1;
	}

	public Integer getSub2Value() {
		return sub2Value;
	}

	public void setSub2Value(Integer sub2Value) {
		this.sub2Value = sub2Value;
	}
}
