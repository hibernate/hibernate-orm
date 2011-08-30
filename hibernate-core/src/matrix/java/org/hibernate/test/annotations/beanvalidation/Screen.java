/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
