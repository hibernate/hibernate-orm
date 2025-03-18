/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;

import org.junit.jupiter.api.Test;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = ModelContributorSmokeTests.SimpleEntity.class
)
public class ModelContributorSmokeTests {
	@Test
	public void simpleModelContributorTest(DomainModelScope scope) {
		final MetadataImplementor domainModel = scope.getDomainModel();
		// Should be 3
		//		1) SimpleEntity
		//		2) Enver's DefaultRevisionEntity
		//		3) Enver's "shadow" of the domain entity (SimpleEntity_AUD)
		assertThat( domainModel.getEntityBindings().size(), is( 3 )  );

		checkModel(
				domainModel.getEntityBinding( SimpleEntity.class.getName() ),
				"orm"
		);

		checkModel(
				domainModel.getEntityBinding( DefaultRevisionEntity.class.getName() ),
				"envers"
		);

		checkModel(
				domainModel.getEntityBinding( SimpleEntity.class.getName() + "_AUD" ),
				"envers"
		);
	}

	private void checkModel(PersistentClass entityBinding, String expectedContributor) {
		assertThat( entityBinding.getContributor(), is( expectedContributor ) );
		assertThat( entityBinding.getRootTable().getContributor(), is( expectedContributor ) );
	}

	@Entity( name = "SimpleEntity" )
	@Table( name = "simple" )
	@Audited
	public static class SimpleEntity {
		@Id
		private Integer id;
		String name;

		public SimpleEntity() {
		}

		public SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		private void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
