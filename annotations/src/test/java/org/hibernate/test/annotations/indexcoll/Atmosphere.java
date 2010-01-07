//$Id$
package org.hibernate.test.annotations.indexcoll;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Column;
import javax.persistence.JoinTable;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.MapKeyTemporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.MapKey;
import org.hibernate.annotations.CollectionOfElements;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Atmosphere {
	@Id
	@GeneratedValue
	public Integer id;

	@ManyToMany(cascade = CascadeType.ALL)
	@MapKeyColumn(name="gas_name")
	public Map<String, Gas> gases = new HashMap<String, Gas>();

	@MapKeyTemporal(TemporalType.DATE)
	@ElementCollection
	public Map<Date, String> colorPerDate = new HashMap<Date,String>();

	@ManyToMany(cascade = CascadeType.ALL)
	@MapKeyJoinColumn(name="gas_id" )
	@JoinTable(name = "Gas_per_key")
	public Map<GasKey, Gas> gasesPerKey = new HashMap<GasKey, Gas>();

	@CollectionOfElements //TODO migrate to @ElementCollection ;  @MapKeyManyToMany ??
	@Column(name="composition_rate")
	@MapKeyJoinColumn(name="gas_id" )
	@JoinTable(name = "Composition", joinColumns = @JoinColumn(name = "atmosphere_id"))
	public Map<Gas, Double> composition = new HashMap<Gas, Double>();

	//use default JPA 2 column name for map key
	@ManyToMany(cascade = CascadeType.ALL)
	@MapKeyColumn
	@JoinTable(name="Atm_Gas_Def")
	public Map<String, Gas> gasesDef = new HashMap<String, Gas>();

	//use default HAN legacy column name for map key
	@ManyToMany(cascade = CascadeType.ALL)
	@MapKey
	@JoinTable(name="Atm_Gas_DefLeg")
	public Map<String, Gas> gasesDefLeg = new HashMap<String, Gas>();

	@ManyToMany(cascade = CascadeType.ALL)
	@MapKeyJoinColumn
	@JoinTable(name = "Gas_p_key_def")
	public Map<GasKey, Gas> gasesPerKeyDef = new HashMap<GasKey, Gas>();

}
