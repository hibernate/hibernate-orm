/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.cid;

import jakarta.persistence.*;

@Entity
@Table(name = "freight")
public class Freight {

	@Id
	@Column(name = "freight_number")
	private Integer freightNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "flight_id", referencedColumnName = "flight_id")
	@JoinColumn(name = "segment_number", referencedColumnName = "segment_number")
	private FlightSegment flightSegment;

	public Integer getFreightNumber() {
		return freightNumber;
	}

	public void setFreightNumber(Integer freightNumber) {
		this.freightNumber = freightNumber;
	}

	public FlightSegment getFlightSegment() {
		return flightSegment;
	}

	public void setFlightSegment(FlightSegment flightSegment) {
		this.flightSegment = flightSegment;
	}

}
