/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.contribution.jts;

import org.hibernate.annotations.JavaType;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

/**
 * @author Steve Ebersole
 */
@Entity(name = "FirePoint")
@Table(name = "FirePoint")
public class FirePoint {
	@Id
	private Integer id;
	@Basic
	private String name;
	@Basic
	@JavaType( PointJavaType.class )
	private Point coordinate;

	private FirePoint() {
	}

	public FirePoint(Integer id, String name, Point coordinate) {
		this.id = id;
		this.name = name;
		this.coordinate = coordinate;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Point getCoordinate() {
		return coordinate;
	}

	public void setCoordinate(Point coordinate) {
		this.coordinate = coordinate;
	}
}