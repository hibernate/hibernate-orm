/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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
