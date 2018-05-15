/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metagen.mappedsuperclass.embeddedid;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * @author Justin Wesley
 * @author Steve Ebersole
 */
@Entity
public class Product extends AbstractProduct {

	private String description;

	public Product() {
	}

	@Column
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
