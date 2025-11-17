/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.identifiercollection;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.TableGenerator;

/**
 * @author Emmanuel Bernard
 */
@Entity
@TableGenerator(name="ids_generator", table="IDS")
public class Passport {
	@Id @GeneratedValue @Column(name="passport_id") private Long id;
	private String name;

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name="PASSPORT_STAMP")
	@CollectionId(column = @Column(name="COLLECTION_ID"), generator = "generator")
	@CollectionIdJdbcTypeCode( Types.BIGINT )
	@TableGenerator(name="generator", table="IDSTAMP")
	private Collection<Stamp> stamps = new ArrayList();

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name="PASSPORT_VISASTAMP")
	@CollectionId(column = @Column(name="COLLECTION_ID"), generator = "ids_generator")
	@CollectionIdJdbcTypeCode( Types.BIGINT )
	//TODO test identity generator
	private Collection<Stamp> visaStamp = new ArrayList();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Collection<Stamp> getStamps() {
		return stamps;
	}

	public void setStamps(Collection<Stamp> stamps) {
		this.stamps = stamps;
	}

	public Collection<Stamp> getVisaStamp() {
		return visaStamp;
	}

	public void setVisaStamp(Collection<Stamp> visaStamp) {
		this.visaStamp = visaStamp;
	}
}
