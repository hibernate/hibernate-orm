/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embeddables.collection;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.orm.junit.JiraKey;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-8860")
public class EmbeddableWithOneToMany_HHH_8860_Test
		extends AbstractEmbeddableWithManyToManyTest {

	@Override
	protected void addAnnotatedClasses(MetadataSources metadataSources) {
		metadataSources.addAnnotatedClasses( Data.class );
	}

	@Entity(name = "Data")
	public static class Data {

		@Id
		private String id;

		@Version
		private Integer version;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@ElementCollection(fetch = FetchType.LAZY)
		@MapKeyColumn(name = "key")
		private Map<String, GenericStringList> stringlist = new TreeMap<>();

	}

	@Embeddable
	public static class GenericStringList {

		@OneToMany(fetch = FetchType.LAZY)
		public List<String> stringList = new LinkedList<>();
	}
}
