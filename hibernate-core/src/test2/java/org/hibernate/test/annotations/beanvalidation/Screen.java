/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.annotations.beanvalidation;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Screen {
	private Integer id;
	private Button stopButton;
	private PowerSupply powerSupply;
	private Set<DisplayConnector> connectors = new HashSet<DisplayConnector>();
	private Set<Color> displayColors = new HashSet<Color>();

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Valid
	public Button getStopButton() {
		return stopButton;
	}

	public void setStopButton(Button stopButton) {
		this.stopButton = stopButton;
	}

	@ManyToOne(cascade = CascadeType.PERSIST)
	@Valid
	@NotNull
	public PowerSupply getPowerSupply() {
		return powerSupply;
	}

	public void setPowerSupply(PowerSupply powerSupply) {
		this.powerSupply = powerSupply;
	}

	@ElementCollection
	@Valid
	public Set<DisplayConnector> getConnectors() {
		return connectors;
	}

	public void setConnectors(Set<DisplayConnector> connectors) {
		this.connectors = connectors;
	}

	@ManyToMany(cascade = CascadeType.PERSIST)
	public Set<Color> getDisplayColors() {
		return displayColors;
	}

	public void setDisplayColors(Set<Color> displayColors) {
		this.displayColors = displayColors;
	}
}
