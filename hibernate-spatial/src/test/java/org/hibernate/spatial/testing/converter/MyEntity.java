/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing.converter;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.geolatte.geom.Geometry;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "SP_CUST_TYPE_CONV_ENTITY")
public class MyEntity {
	private Integer id;
	private Geometry geometry;

	public MyEntity() {
	}

	public MyEntity(Integer id, Geometry geometry) {
		this.id = id;
		this.geometry = geometry;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	// NOTE : the AttributeConverter should be auto-applied here

	@Basic
	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}
}
