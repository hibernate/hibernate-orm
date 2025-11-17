/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll.eager;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.TemporalType;
import org.hibernate.orm.test.annotations.indexcoll.Gas;
import org.hibernate.orm.test.annotations.indexcoll.GasKey;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Atmosphere {

	public enum Level {
		LOW,
		HIGH
	}

	@Id
	@GeneratedValue
	public Integer id;

	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@MapKeyColumn(name = "gas_name")
	public Map<String, Gas> gases = new HashMap<>();

	@MapKeyTemporal(TemporalType.DATE)
	@ElementCollection(fetch = FetchType.EAGER)
	@MapKeyColumn(nullable = false)
	public Map<Date, String> colorPerDate = new HashMap<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@MapKeyEnumerated(EnumType.STRING)
	@MapKeyColumn(nullable = false)
	public Map<Level, String> colorPerLevel = new HashMap<>();

	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@MapKeyJoinColumn(name = "gas_id")
	@JoinTable(name = "Gas_per_key")
	public Map<GasKey, Gas> gasesPerKey = new HashMap<GasKey, Gas>();

	@ElementCollection(fetch = FetchType.EAGER)
	@Column(name = "composition_rate")
	@MapKeyJoinColumns({@MapKeyJoinColumn(name = "gas_id")}) //use @MapKeyJoinColumns explicitly for tests
	@JoinTable(name = "Composition", joinColumns = @JoinColumn(name = "atmosphere_id"))
	public Map<Gas, Double> composition = new HashMap<>();

	//use default JPA 2 column name for map key
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@MapKeyColumn
	@JoinTable(name = "Atm_Gas_Def")
	public Map<String, Gas> gasesDef = new HashMap<>();

	//use default HAN legacy column name for map key
	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@MapKeyColumn
	@JoinTable(name = "Atm_Gas_DefLeg")
	public Map<String, Gas> gasesDefLeg = new HashMap<>();

	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@MapKeyJoinColumn
	@JoinTable(name = "Gas_p_key_def")
	public Map<GasKey, Gas> gasesPerKeyDef = new HashMap<>();

}
