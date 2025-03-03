/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.query;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.Table;

/**
 * Example of a entity load incl a join fetching of an associated *ToOne entity
 *
 * @author Emmanuel Bernard
 */
@Entity
@NamedNativeQueries({
@NamedNativeQuery(
		name = "night&area", query = "select night.id as nid, night.night_duration, night.night_date, area.id as aid, "
		+ "night.area_id, area.name from Night night, tbl_area area where night.area_id = area.id",
		resultSetMapping = "joinMapping")
		})
@org.hibernate.annotations.NamedNativeQueries({
@org.hibernate.annotations.NamedNativeQuery(
		name = "night&areaCached",
		query = "select night.id as nid, night.night_duration, night.night_date, area.id as aid, "
				+ "night.area_id, area.name from Night night, tbl_area area where night.area_id = area.id",
		resultSetMapping = "joinMapping")
		})
@SqlResultSetMappings(
		@SqlResultSetMapping(name = "joinMapping", entities = {
		@EntityResult(entityClass = Night.class, fields = {
		@FieldResult(name = "id", column = "nid"),
		@FieldResult(name = "duration", column = "night_duration"),
		@FieldResult(name = "date", column = "night_date"),
		@FieldResult(name = "area", column = "area_id")
				}),
		@EntityResult(entityClass = Area.class, fields = {
		@FieldResult(name = "id", column = "aid"),
		@FieldResult(name = "name", column = "name")
				})
				}
		)
)
@Table(name = "tbl_area")
public class Area {
	private Integer id;
	private String name;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(unique = true, nullable=false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
