/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement.recreate;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.*;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author Sergey Astakhov
 */
@Entity
@GenericGenerator(name = "increment", strategy = "increment")
public class RaceExecution {

	@Id
	@GeneratedValue
	private Integer id;

	@ElementCollection
	@MapKeyClass(Poi.class)
	@MapKeyJoinColumn(name = "poi", nullable = false)
	@CollectionTable(name = "race_poi_arrival", joinColumns = @JoinColumn(name = "race_id"))
	private Map<Poi, PoiArrival> poiArrival;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Map<Poi, PoiArrival> getPoiArrival() {
		return poiArrival;
	}

	public void setPoiArrival(Map<Poi, PoiArrival> _poiArrival) {
		poiArrival = _poiArrival;
	}

	public void arriveToPoi(Poi poi, Date time) {
		if ( poiArrival == null ) {
			poiArrival = new HashMap<Poi, PoiArrival>();
		}

		PoiArrival arrival = poiArrival.get( poi );
		if ( arrival == null ) {
			arrival = new PoiArrival();
			poiArrival.put( poi, arrival );
		}

		arrival.setArriveTime( time );
	}

	public void expectedArrive(Poi poi, Date time) {
		if ( poiArrival == null ) {
			poiArrival = new HashMap<Poi, PoiArrival>();
		}

		PoiArrival arrival = poiArrival.get( poi );
		if ( arrival == null ) {
			arrival = new PoiArrival();
			poiArrival.put( poi, arrival );
		}

		arrival.setExpectedTime( time );
	}

}
