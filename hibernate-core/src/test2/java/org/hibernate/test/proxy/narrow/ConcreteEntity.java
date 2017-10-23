/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.proxy.narrow;

import javax.persistence.Entity;

/**
 * @author Yoann Rodière
 * @author Guillaume Smet
 */
@Entity
public class ConcreteEntity extends AbstractEntity {

	private String content = "text";

	public ConcreteEntity() {
		super();
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}
