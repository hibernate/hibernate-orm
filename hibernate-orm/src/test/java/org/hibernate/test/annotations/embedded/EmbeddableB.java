/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embedded;

import javax.persistence.Embeddable;

/**
 * @author Brett Meyer
 */
@Embeddable
public class EmbeddableB {
	
	private String embedAttrB;

	public String getEmbedAttrB() {
		return embedAttrB;
	}

	public void setEmbedAttrB(String embedAttrB) {
		this.embedAttrB = embedAttrB;
	}
}
