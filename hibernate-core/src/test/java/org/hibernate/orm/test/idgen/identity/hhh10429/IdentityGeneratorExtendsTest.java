/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.identity.hhh10429;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
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

	@IdGeneratorType( CustomIdentityGenerator.class )
	@Retention( RUNTIME )
	@Target( { FIELD, METHOD } )
	public @interface CustomIdentity {
	}

	@Entity(name = "EntityBean")
	@Table( name = "entity_bean" )
	public static class EntityBean {

		@Id
		@Column
		@CustomIdentity
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
