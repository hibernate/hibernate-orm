/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.polymorphism;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.PolymorphismType;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
@org.hibernate.annotations.Entity(polymorphism = PolymorphismType.EXPLICIT)
public class Car extends Automobile {
	
	@Id
	@GeneratedValue
	private long id;

	private String model;
	
	@ManyToOne
	// purposefully refer to a non-PK column (HHH-7915)
	@JoinColumn( referencedColumnName = "REGION_CODE")
	private MarketRegion marketRegion;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public MarketRegion getMarketRegion() {
		return marketRegion;
	}

	public void setMarketRegion(MarketRegion marketRegion) {
		this.marketRegion = marketRegion;
	}
}
