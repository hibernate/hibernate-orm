/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.spatial.testing.converter;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.geolatte.geom.Geometry;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "SP_CUST_TYPE_CONV_ENTITY")
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
