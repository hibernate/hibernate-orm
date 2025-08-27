/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class LegalStructure {
	private String name;
	private String country;
	private CorpType corporationType;
	private Nationality origin;
	private Set<Manager> topManagement = new HashSet<Manager>();

	@ManyToOne
	@JoinColumn(name = "CORP_ID")
	public CorpType getCorporationType() {
		return corporationType;
	}

	public void setCorporationType(CorpType corporationType) {
		this.corporationType = corporationType;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	@JoinColumn(name = "origin_fk")
	public Nationality getOrigin() {
		return origin;
	}

	public void setOrigin(Nationality origin) {
		this.origin = origin;
	}

	@OneToMany(mappedBy = "employer")
	public Set<Manager> getTopManagement() {
		return topManagement;
	}

	public void setTopManagement(Set<Manager> topManagement) {
		this.topManagement = topManagement;
	}
}
