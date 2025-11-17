/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone.foreignkey;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity(name = "RootLayer")
@Audited
public class RootLayer {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	@OneToMany(mappedBy = "rootLayer", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<MiddleLayer> middleLayers;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<MiddleLayer> getMiddleLayers() {
		return middleLayers;
	}

	public void setMiddleLayers(List<MiddleLayer> middleLayers) {
		this.middleLayers = middleLayers;
	}
}
