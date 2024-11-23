/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.lazytoone;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.LazyToOne;

import static javax.persistence.FetchType.LAZY;
import static org.hibernate.annotations.LazyToOneOption.NO_PROXY;

/**
 * @author Steve Ebersole
 */
@Entity( name = "Flight" )
@Table( name = "flight" )
public class Flight {
	@Id
	private Integer id;
	@Column( name = "flight_number" )
	private String number;

	@ManyToOne( fetch = LAZY )
	private Airport origination;

	@ManyToOne( fetch = LAZY )
	@LazyToOne( NO_PROXY )
	private Airport destination;

	public Flight() {
	}

	public Flight(Integer id, String number) {
		this.id = id;
		this.number = number;
	}

	public Flight(Integer id, String number, Airport origination, Airport destination) {
		this.id = id;
		this.number = number;
		this.origination = origination;
		this.destination = destination;
	}

	public Integer getId() {
		return id;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public Airport getOrigination() {
		return origination;
	}

	public void setOrigination(Airport origination) {
		this.origination = origination;
	}

	public Airport getDestination() {
		return destination;
	}

	public void setDestination(Airport destination) {
		this.destination = destination;
	}
}
