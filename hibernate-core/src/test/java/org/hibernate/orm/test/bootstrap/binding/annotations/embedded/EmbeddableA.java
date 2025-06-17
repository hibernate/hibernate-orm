/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

/**
 * @author Brett Meyer
 */
@Embeddable
public class EmbeddableA {

	@Embedded
	@AttributeOverrides({@AttributeOverride(name = "embedAttrB" , column = @Column(table = "TableB"))})
	private EmbeddableB embedB;
	@Column(table = "TableB")
	private String embedAttrA;

	public EmbeddableB getEmbedB() {
		return embedB;
	}

	public void setEmbedB(EmbeddableB embedB) {
		this.embedB = embedB;
	}

	public String getEmbedAttrA() {
		return embedAttrA;
	}

	public void setEmbedAttrA(String embedAttrA) {
		this.embedAttrA = embedAttrA;
	}

}
