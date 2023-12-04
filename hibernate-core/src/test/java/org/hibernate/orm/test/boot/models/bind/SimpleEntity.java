/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.annotations.TenantId;

import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A simple entity for initial work on the table
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "simpletons", comment = "Stupid is as stupid does")
@SecondaryTable(name = "simple_stuff", schema = "my_schema", catalog = "my_catalog", comment = "Don't sweat it")
@FilterDef(name = "by-name", defaultCondition = "name = :name", parameters = @ParamDef( name = "name", type = String.class ) )
@Filter(name = "by-name")
@SoftDelete(strategy = SoftDeleteType.ACTIVE)
@Cacheable(false)
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region = "my-region")
public class SimpleEntity {
	@Id
	private Integer id;
	@Basic
	private String name;
	@Basic
	@Column(table = "simple_stuff", name = "datum")
	private String data;

	@TenantId
	private String tenantKey;

	@Version
	private int version;

	@Enumerated(EnumType.STRING)
	private Stuff stuff;

	enum Stuff {BITS, BOBS}

	protected SimpleEntity() {
		// for Hibernate use
	}

	public SimpleEntity(Integer id, String name) {
		this.id = id;
		this.name = name;
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

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
