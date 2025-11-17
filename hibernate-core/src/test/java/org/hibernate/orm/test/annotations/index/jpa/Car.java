/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.index.jpa;

import java.util.List;
import java.util.Set;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;


/**
 * @author Strong Liu
 */
@Entity
@Table(indexes = {
		@Index(unique = true, columnList = "brand, producer"),
		@Index(name = "Car_idx", columnList = "since DESC")
})
@SecondaryTable(name = "T_DEALER", indexes = @Index(columnList = "dealer_name ASC, rate DESC"))
public class Car {
	@Id
	private long id;
	private String brand;
	private String producer;
	private long since;
	@AttributeOverride(name = "name", column = @Column(name = "dealer_name", table = "T_DEALER"))
	@AttributeOverride(name = "rate", column = @Column(table = "T_DEALER"))
	@Embedded
	private Dealer dealer;

	@ElementCollection
	@CollectionTable(name = "CAR_DEALTERS", indexes = @Index(columnList = "name"))
	private Set<Dealer> otherDealers;


	@ManyToMany(cascade = CascadeType.ALL, mappedBy = "cars")
	private List<Importer> importers;

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public Dealer getDealer() {
		return dealer;
	}

	public void setDealer(Dealer dealer) {
		this.dealer = dealer;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<Importer> getImporters() {
		return importers;
	}

	public void setImporters(List<Importer> importers) {
		this.importers = importers;
	}

	public Set<Dealer> getOtherDealers() {
		return otherDealers;
	}

	public void setOtherDealers(Set<Dealer> otherDealers) {
		this.otherDealers = otherDealers;
	}

	public String getProducer() {
		return producer;
	}

	public void setProducer(String producer) {
		this.producer = producer;
	}

	public long getSince() {
		return since;
	}

	public void setSince(long since) {
		this.since = since;
	}
}
