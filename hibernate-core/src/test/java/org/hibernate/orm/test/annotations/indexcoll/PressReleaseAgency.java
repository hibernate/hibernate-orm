/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKey;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class PressReleaseAgency {
	private Integer id;
	private String name;
	private Map<Integer, News> providedNews = new HashMap<Integer, News>();

	@Id
	@GeneratedValue
	@Column(name = "PressReleaseAgency_id")
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

	@ManyToMany
	@JoinTable(joinColumns = @JoinColumn(name = "PressReleaseAgency_id"),
			inverseJoinColumns = @JoinColumn(name = "News_id"))
	@MapKey
	public Map<Integer, News> getProvidedNews() {
		return providedNews;
	}

	public void setProvidedNews(Map<Integer, News> providedNews) {
		this.providedNews = providedNews;
	}
}
