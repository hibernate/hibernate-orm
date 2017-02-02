/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.query;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;

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
		@EntityResult(entityClass = org.hibernate.test.annotations.query.Night.class, fields = {
		@FieldResult(name = "id", column = "nid"),
		@FieldResult(name = "duration", column = "night_duration"),
		@FieldResult(name = "date", column = "night_date"),
		@FieldResult(name = "area", column = "area_id")
				}),
		@EntityResult(entityClass = org.hibernate.test.annotations.query.Area.class, fields = {
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
