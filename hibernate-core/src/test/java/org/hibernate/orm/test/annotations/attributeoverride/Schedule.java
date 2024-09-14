/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.attributeoverride;

import java.time.LocalTime;
import java.util.Map;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Schedule {

	@Id
	@GeneratedValue
	private Long id;

	@ElementCollection
	@Column(name = "time")
	@AttributeOverride(name = "key.origin", column = @Column(name = "orig"))
	@AttributeOverride(name = "key.destination", column = @Column(name = "dest"))
	private Map<Route, LocalTime> departures;

	public Schedule() {
	}

	public Schedule(Map<Route, LocalTime> departures) {
		this.departures = departures;
	}
}
