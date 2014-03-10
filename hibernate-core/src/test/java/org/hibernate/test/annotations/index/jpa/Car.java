/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.index.jpa;

import java.util.List;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;
import javax.persistence.TableGenerator;


/**
 * @author Strong Liu <stliu@hibernate.org>
 */
@Entity
@Table(indexes = { @Index(unique = true, columnList = "brand, producer"),
		@Index(name = "Car_idx", columnList = "since DESC")
})
@TableGenerator(name = "idGen", table = "ID_GEN", valueColumnName="GEN_VALUE",
		indexes = @Index(columnList = "GEN_VALUE"))
@SecondaryTable(name = "T_DEALER", indexes = @Index(columnList = "dealer_name ASC, rate DESC"))
public class Car {
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "idGen")
	private long id;
	
	private String brand;
	
	private String producer;
	
	private long since;
	
	@AttributeOverrides({
			@AttributeOverride(name = "name", column = @Column(name = "dealer_name", table = "T_DEALER")),
			@AttributeOverride(name = "rate", column = @Column(table = "T_DEALER"))
	})
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
