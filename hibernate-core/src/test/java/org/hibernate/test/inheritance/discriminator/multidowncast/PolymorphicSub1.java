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
public class PolymorphicSub1 extends PolymorphicBase {
	private static final long serialVersionUID = 1L;

	private IntIdEntity relation1;
	private PolymorphicBase parent1;
	private NameObject embeddable1;
	private Integer sub1Value;

	public PolymorphicSub1() {
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public IntIdEntity getRelation1() {
		return relation1;
	}

	public void setRelation1(IntIdEntity relation1) {
		this.relation1 = relation1;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public PolymorphicBase getParent1() {
		return parent1;
	}

	public void setParent1(PolymorphicBase parent1) {
		this.parent1 = parent1;
	}

	@Embedded
	public NameObject getEmbeddable1() {
		return embeddable1;
	}

	public void setEmbeddable1(NameObject embeddable1) {
		this.embeddable1 = embeddable1;
	}

	public Integer getSub1Value() {
		return sub1Value;
	}

	public void setSub1Value(Integer sub1Value) {
		this.sub1Value = sub1Value;
	}
}
