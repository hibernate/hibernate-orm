/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.NaturalId;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name="web_app" )
public class WebApplication extends BaseEntity {
	private String name;
	private String siteUrl;

	private Set<Activity> activities = new HashSet<>();

	@SuppressWarnings("unused")
	public WebApplication() {
	}

	public WebApplication(Integer id, String siteUrl) {
		super( id );
		this.siteUrl = siteUrl;
	}

	@NaturalId
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Basic( fetch = FetchType.LAZY )
	public String getSiteUrl() {
		return siteUrl;
	}

	public void setSiteUrl(String siteUrl) {
		this.siteUrl = siteUrl;
	}

	@OneToMany(mappedBy="webApplication", fetch= FetchType.LAZY)
	@LazyGroup("app_activity_group")
//	@CollectionType(type="baseutil.technology.hibernate.IskvLinkedSetCollectionType")
	public Set<Activity> getActivities() {
		return activities;
	}

	public void setActivities(Set<Activity> activities) {
		this.activities = activities;
	}
}
