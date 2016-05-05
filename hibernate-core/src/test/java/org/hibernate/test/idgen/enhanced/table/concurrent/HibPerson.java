/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.enhanced.table.concurrent;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/**
 * @author Richard Barnes 4 May 2016
 */
@Entity
public class HibPerson {

	@Id
	@GeneratedValue(generator = "HIB_TGEN")
	@GenericGenerator(name = "HIB_TGEN", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = {
			@Parameter(name = "table_name", value = "HIB_TGEN"),
			@Parameter(name = "prefer_entity_table_as_segment_value", value = "true"),
			@Parameter(name = "optimizer", value = "hilo"),
			@Parameter(name = "initial_value", value = "1"),
			@Parameter(name = "increment_size", value = "5") })
	private long id = -1;

	public HibPerson() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

}
