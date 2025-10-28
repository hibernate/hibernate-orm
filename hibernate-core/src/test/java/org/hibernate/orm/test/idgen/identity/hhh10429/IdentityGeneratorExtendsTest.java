/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.identity.hhh10429;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Matthew Morrissette
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-10429")
@DomainModel( annotatedClasses = IdentityGeneratorExtendsTest.EntityBean.class )
public class IdentityGeneratorExtendsTest {

	@Test
	public void testIdentifierGeneratorExtendsIdentityGenerator(DomainModelScope scope) {
		final MetadataImplementor domainModel = scope.getDomainModel();
		final PersistentClass entityBinding = domainModel.getEntityBinding( EntityBean.class.getName() );
		final KeyValue identifier = entityBinding.getIdentifier();

		assertTrue( identifier.getColumns().get(0).isIdentity() );
	}

	@Entity(name = "EntityBean")
	@Table( name = "entity_bean" )
	public static class EntityBean {

		@Id
		@Column
		@GeneratedValue(strategy = GenerationType.IDENTITY, generator = "customGenerator")
		@GenericGenerator(name = "customGenerator", strategy = "org.hibernate.orm.test.idgen.identity.hhh10429.CustomIdentityGenerator")
		private int entityId;

		public int getEntityId() {
			return entityId;
		}

		public void setEntityId(int entityId) {
			this.entityId = entityId;
		}

		public String description;
	}
}
