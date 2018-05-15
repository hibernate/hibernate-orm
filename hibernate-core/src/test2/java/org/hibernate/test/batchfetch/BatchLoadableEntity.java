/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.batchfetch;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.BatchSize;

/**
 * @author Steve Ebersole
 */
@Entity
@BatchSize( size = 32 )
public class BatchLoadableEntity {
	private Integer id;
	private String name;

	public BatchLoadableEntity() {
	}

	public BatchLoadableEntity(int id) {
		this.id = id;
		this.name = "Entity #" + id;
	}

	@Id
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
}
