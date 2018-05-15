/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metagen.mappedsuperclass.overridden;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Oliver Breidenbach
 */
@Entity
@Table(name = "product")
@Access(AccessType.PROPERTY)
public class Product1 extends AbstractProduct {

	private String overridenName;

	public Product1() {
	}

	public Product1(String name) {
		super( name );
	}


	@Column(name = "overridenName")
	public String getOverridenName() {
		return overridenName;
	}

	public void setOverridenName(String overridenName) {
		this.overridenName = overridenName;
	}
}
