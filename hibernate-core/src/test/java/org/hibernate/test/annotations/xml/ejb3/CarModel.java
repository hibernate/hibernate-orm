//$Id$
package org.hibernate.test.annotations.xml.ejb3;
import java.util.Date;

/**
 * @author Emmanuel Bernard
 */
public class CarModel extends Model {
	private Date year;

	public Date getYear() {
		return year;
	}

	public void setYear(Date year) {
		this.year = year;
	}
}
