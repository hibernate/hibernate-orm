/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql.size.filter;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.SQLRestriction;

@Entity
@SQLRestriction("deleted = false")
public class City {

	@Id
	private Integer id;

	private String name;

	@ManyToOne
	private Region region;

	private Boolean deleted;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}
}
