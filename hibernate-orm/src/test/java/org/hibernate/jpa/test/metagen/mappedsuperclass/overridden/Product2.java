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

/**
 * @author Oliver Breidenbach
 */
@Entity
@Access(AccessType.PROPERTY)
public class Product2 extends Product1 {

	@Column(name = "overridenName"/*, insertable = false, updatable = false*/)
	public String getOverridenName() {
		return super.getOverridenName();
	}

	public void setOverridenName(String overridenName) {
		super.setOverridenName(overridenName);
	}
}
