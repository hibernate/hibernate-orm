/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;

import java.sql.Types;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Use hibernate specific annotations
 *
 * @author Emmanuel Bernard
 */
@Entity
@BatchSize(size = 5)
@DynamicInsert @DynamicUpdate
@OptimisticLocking(type = OptimisticLockType.ALL)
@SQLRestriction("1=1")
@FilterDef(name = "minLength", parameters = {@ParamDef(name = "minLength", type = Integer.class)})
@Filter(name = "betweenLength")
@Filter(name = "minLength", condition = ":minLength <= length")
@Table(indexes = @Index(name = "idx", columnList = "name, length"))
public class Forest {
	private Integer id;
	private String name;
	private long length;
	private String longDescription;
	private String smallText;
	private String bigText;
	private Country country;
	private Set<Country> near;

	@OptimisticLock(excluded=true)
	@JdbcTypeCode( Types.LONGVARCHAR )
	@Column(length = 10000)
	public String getLongDescription() {
		return longDescription;
	}

	public void setLongDescription(String longDescription) {
		this.longDescription = longDescription;
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	@Id
	@GeneratedValue
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

	@Convert( converter = ToLowerConverter.class )
	public String getSmallText() {
		return smallText;
	}

	@Convert( converter = ToUpperConverter.class )
	public String getBigText() {
		return bigText;
	}

	public void setSmallText(String smallText) {
		this.smallText = smallText;
	}

	public void setBigText(String bigText) {
		this.bigText = bigText;
	}

	@Lob
	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	@Lob
	@ElementCollection
	public Set<Country> getNear() {
		return near;
	}

	public void setNear(Set<Country> near) {
		this.near = near;
	}

}
