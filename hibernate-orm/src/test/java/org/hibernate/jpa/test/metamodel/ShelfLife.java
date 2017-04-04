/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel;
import java.sql.Date;
import javax.persistence.Basic;
import javax.persistence.Embeddable;

@Embeddable
public class ShelfLife implements java.io.Serializable {
    private Date inceptionDate;
    private Date soldDate;

	public ShelfLife() {
	}

	public ShelfLife(Date inceptionDate, Date soldDate) {
		this.inceptionDate = inceptionDate;
		this.soldDate = soldDate;
	}

	@Basic
	public Date getInceptionDate() {
		return inceptionDate;
	}

	public void setInceptionDate(Date inceptionDate) {
		this.inceptionDate = inceptionDate;
	}

	@Basic
	public Date getSoldDate() {
		return soldDate;
	}

	public void setSoldDate(Date soldDate) {
		this.soldDate = soldDate;
	}
}
