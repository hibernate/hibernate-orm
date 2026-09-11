/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.readonly;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * @author Steve Ebersole, Gail Badner (adapted this from "proxy" tests version)
 */
@Entity
public class Container implements Serializable {
	@Id
	@GeneratedValue
	private Long id;
	private String name;
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "no_proxy_owner_name", referencedColumnName = "name")
	private Owner noProxyOwner;
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "proxy_owner_name", referencedColumnName = "name")
	private Owner proxyOwner;
	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "non_lazy_owner_name", referencedColumnName = "name")
	private Owner nonLazyOwner;
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "no_proxy_info_id")
	private Info noProxyInfo;
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "proxy_info_id")
	private Info proxyInfo;
	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "non_lazy_info_id")
	private Info nonLazyInfo;
	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "c_lazy_id")
	private Set<DataPoint> lazyDataPoints = new HashSet<>();
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "c_non_lazy_join_id")
	@Fetch(FetchMode.JOIN)
	private Set<DataPoint> nonLazyJoinDataPoints = new HashSet<>();
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "c_non_lazy_select_id")
	@Fetch(FetchMode.SELECT)
	private Set<DataPoint> nonLazySelectDataPoints = new HashSet<>();

	public Container() {
	}

	public Container(String name) {
		this.name = name;
	}

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

	public Owner getNoProxyOwner() {
		return noProxyOwner;
	}

	public void setNoProxyOwner(Owner noProxyOwner) {
		this.noProxyOwner = noProxyOwner;
	}

	public Owner getProxyOwner() {
		return proxyOwner;
	}

	public void setProxyOwner(Owner proxyOwner) {
		this.proxyOwner = proxyOwner;
	}

	public Owner getNonLazyOwner() {
		return nonLazyOwner;
	}

	public void setNonLazyOwner(Owner nonLazyOwner) {
		this.nonLazyOwner = nonLazyOwner;
	}

	public Info getNoProxyInfo() {
		return noProxyInfo;
	}

	public void setNoProxyInfo(Info noProxyInfo) {
		this.noProxyInfo = noProxyInfo;
	}

	public Info getProxyInfo() {
		return proxyInfo;
	}

	public void setProxyInfo(Info proxyInfo) {
		this.proxyInfo = proxyInfo;
	}

	public Info getNonLazyInfo() {
		return nonLazyInfo;
	}

	public void setNonLazyInfo(Info nonLazyInfo) {
		this.nonLazyInfo = nonLazyInfo;
	}

	public Set<DataPoint> getLazyDataPoints() {
		return lazyDataPoints;
	}

	public void setLazyDataPoints(Set<DataPoint> lazyDataPoints) {
		this.lazyDataPoints = lazyDataPoints;
	}

	public Set<DataPoint> getNonLazyJoinDataPoints() {
		return nonLazyJoinDataPoints;
	}

	public void setNonLazyJoinDataPoints(Set<DataPoint> nonLazyJoinDataPoints) {
		this.nonLazyJoinDataPoints = nonLazyJoinDataPoints;
	}

	public Set<DataPoint> getNonLazySelectDataPoints() {
		return nonLazySelectDataPoints;
	}

	public void setNonLazySelectDataPoints(Set<DataPoint> nonLazySelectDataPoints) {
		this.nonLazySelectDataPoints = nonLazySelectDataPoints;
	}
}
