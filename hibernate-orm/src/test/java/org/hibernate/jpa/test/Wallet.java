/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Wallet implements Serializable {
	private String serial;
	private String model;
	private Date marketEntrance;
	private String brand;

	@Id
	public String getSerial() {
		return serial;
	}

	public void setSerial(String serial) {
		this.serial = serial;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public Date getMarketEntrance() {
		return marketEntrance;
	}

	public void setMarketEntrance(Date marketEntrance) {
		this.marketEntrance = marketEntrance;
	}
}
