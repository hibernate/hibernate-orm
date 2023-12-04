/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.process;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Sub")
@Table(name = "subs")
@Access(AccessType.PROPERTY)
public class Sub extends Root {
	private String moreDetails;

	@Basic
	public String getMoreDetails() {
		return moreDetails;
	}

	public void setMoreDetails(String moreDetails) {
		this.moreDetails = moreDetails;
	}
}
