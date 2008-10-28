//$Id$
package org.hibernate.test.annotations.indexcoll;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Column;
import javax.persistence.JoinTable;
import javax.persistence.JoinColumn;

import org.hibernate.annotations.MapKey;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.MapKeyManyToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Atmosphere {
	@Id
	@GeneratedValue
	public Integer id;

	@ManyToMany(cascade = CascadeType.ALL)
	@MapKey(columns = {@Column(name="gas_name")})
	public Map<String, Gas> gases = new HashMap<String, Gas>();

	@ManyToMany(cascade = CascadeType.ALL)
	@MapKeyManyToMany(joinColumns = @JoinColumn(name="gas_id") )
	@JoinTable(name = "Gas_per_key")
	public Map<GasKey, Gas> gasesPerKey = new HashMap<GasKey, Gas>();

	@CollectionOfElements
	@Column(name="composition_rate")
	@MapKeyManyToMany(joinColumns = @JoinColumn(name="gas_id"))
	@JoinTable(name = "Composition", joinColumns = @JoinColumn(name = "atmosphere_id"))
	public Map<Gas, Double> composition = new HashMap<Gas, Double>();
}
