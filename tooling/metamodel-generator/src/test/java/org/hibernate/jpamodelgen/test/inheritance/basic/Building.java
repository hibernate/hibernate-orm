/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.inheritance.basic;

import java.sql.Date;
import javax.persistence.MappedSuperclass;


/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Building extends Area {
	public Date getBuiltIn() {
		return builtIn;
	}

	public void setBuiltIn(Date builtIn) {
		this.builtIn = builtIn;
	}

	private Date builtIn;
}
