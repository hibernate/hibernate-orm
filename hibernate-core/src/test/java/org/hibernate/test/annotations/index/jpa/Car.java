/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;


/**
 * @author <a href="mailto:stliu@hibernate.org">Strong Liu</a>
 */
@Entity
@Table(indexes = {
		@Index(unique = true, columnList = "brand, producer")
		, @Index(name = "Car_idx", columnList = "since DESC")
})
@SecondaryTable(name = "T_DEALER", indexes = @Index(columnList = "dealer_name ASC, rate DESC"))
public class Car {
	@Id
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
