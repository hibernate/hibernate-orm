/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
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

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Atmosphere {

	public static enum Level {
		LOW,
		HIGH
	}

	@Id
	@GeneratedValue
	public Integer id;

	@ManyToMany(cascade = CascadeType.ALL)
	@MapKeyColumn(name="gas_name")
	public Map<String, Gas> gases = new HashMap<String, Gas>();

	@MapKeyTemporal(TemporalType.DATE)
	@ElementCollection
	@MapKeyColumn(nullable=false)
	public Map<Date, String> colorPerDate = new HashMap<Date,String>();

	@ElementCollection
	@MapKeyEnumerated(EnumType.STRING)
	@MapKeyColumn(nullable=false)
	public Map<Level, String> colorPerLevel = new HashMap<Level,String>();

	@ManyToMany(cascade = CascadeType.ALL)
	@MapKeyJoinColumn(name="gas_id" )
	@JoinTable(name = "Gas_per_key")
	public Map<GasKey, Gas> gasesPerKey = new HashMap<GasKey, Gas>();

	@ElementCollection
	@Column(name="composition_rate")
	@MapKeyJoinColumns( { @MapKeyJoinColumn(name="gas_id" ) } ) //use @MapKeyJoinColumns explicitly for tests
	@JoinTable(name = "Composition", joinColumns = @JoinColumn(name = "atmosphere_id"))
	public Map<Gas, Double> composition = new HashMap<Gas, Double>();

	//use default JPA 2 column name for map key
	@ManyToMany(cascade = CascadeType.ALL)
	@MapKeyColumn
	@JoinTable(name="Atm_Gas_Def")
	public Map<String, Gas> gasesDef = new HashMap<String, Gas>();

	//use default HAN legacy column name for map key
	@ManyToMany(cascade = CascadeType.ALL)
	@MapKeyColumn
	@JoinTable(name="Atm_Gas_DefLeg")
	public Map<String, Gas> gasesDefLeg = new HashMap<String, Gas>();

	@ManyToMany(cascade = CascadeType.ALL)
	@MapKeyJoinColumn
	@JoinTable(name = "Gas_p_key_def")
	public Map<GasKey, Gas> gasesPerKeyDef = new HashMap<GasKey, Gas>();

}
