/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations.any;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.NotImplementedYet;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


/**
 * The annotation @FilterJoinTable is incorrectly not accepted by @ManyToAny, which always utilizes a @JoinTable.
 *
 * @author Vincent Bouthinon
 */
@ServiceRegistry()
@JiraKey("HHH-19285")
class ManyToAnyWithFilterJoinTableTest {

	@NotImplementedYet
	@Test
	void testManyToAnyWithFilterJoinTable(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( ObjectTest.class );
		metadataSources.buildMetadata();
	}

	@Entity
	public static class ObjectTest {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToAny
		@AnyKeyJavaClass( Long.class )
		@JoinTable(name = "linkTable", joinColumns = @JoinColumn(name = "SOURCE"), inverseJoinColumns = @JoinColumn(name = "DEST"))
		@FilterJoinTable(name= "filter", condition = "1=1")
		private List<Object> list = new ArrayList<>();
	}
}
