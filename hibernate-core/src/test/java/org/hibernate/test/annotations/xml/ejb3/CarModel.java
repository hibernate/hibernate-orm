/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.xml.ejb3;
import javax.persistence.Column;
import java.util.Date;

/**
 * @author Emmanuel Bernard
 */
public class CarModel extends Model {
	@Column(name="`year`")
	private Date year;

	public Date getYear() {
		return year;
	}

	public void setYear(Date year) {
		this.year = year;
	}
}
