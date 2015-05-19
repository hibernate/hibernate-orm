/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.various;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@org.hibernate.annotations.Table(appliesTo = "Vehicule",
		indexes = {
		@Index(name = "improbableindex", columnNames = {"registration", "Conductor_fk"}),
		@Index(name = "secondone", columnNames = {"Conductor_fk"})
				}
)
public class Vehicule {
	@Id
	@GeneratedValue(generator = "gen")
	@GenericGenerator(name = "gen", strategy = "uuid")
	private String id;
	@Column(name = "registration")
	private String registrationNumber;
	@ManyToOne(optional = false)
	@JoinColumn(name = "Conductor_fk")
	@Index(name = "thirdone")
	private Conductor currentConductor;
	@Index(name = "year_idx")
	@Column(name = "`year`")
	private Integer year;
	@ManyToOne(optional = true)
	@Index(name = "forthone")
	private Conductor previousConductor;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRegistrationNumber() {
		return registrationNumber;
	}

	public void setRegistrationNumber(String registrationNumber) {
		this.registrationNumber = registrationNumber;
	}

	public Conductor getCurrentConductor() {
		return currentConductor;
	}

	public void setCurrentConductor(Conductor currentConductor) {
		this.currentConductor = currentConductor;
	}

	public Integer getYear() {
		return year;
	}

	public void setYear(Integer year) {
		this.year = year;
	}

	public Conductor getPreviousConductor() {
		return previousConductor;
	}

	public void setPreviousConductor(Conductor previousConductor) {
		this.previousConductor = previousConductor;
	}
}
