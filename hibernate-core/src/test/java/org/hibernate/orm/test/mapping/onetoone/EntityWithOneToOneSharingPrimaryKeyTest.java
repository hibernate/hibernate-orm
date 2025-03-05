/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.domain.gambit.EntityWithOneToOneSharingPrimaryKey;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EntityWithOneToOneSharingPrimaryKey.class,
				SimpleEntity.class
		}
)
@ServiceRegistry
@SessionFactory
public class EntityWithOneToOneSharingPrimaryKeyTest {
	@Test
	public void basicTest(SessionFactoryScope scope) {
		final EntityPersister entityDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.findEntityDescriptor( EntityWithOneToOneSharingPrimaryKey.class );

		final ModelPart otherAssociation = entityDescriptor.findSubPart( "other" );

		assertThat( otherAssociation, instanceOf( ToOneAttributeMapping.class ) );

		final ToOneAttributeMapping otherAttributeMapping = (ToOneAttributeMapping) otherAssociation;

		ForeignKeyDescriptor foreignKeyDescriptor = otherAttributeMapping.getForeignKeyDescriptor();
		foreignKeyDescriptor.visitKeySelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "entity_o2o_sharepk" ) );
					assertThat( selection.getSelectionExpression(), is( "id" ) );
				}
		);

		foreignKeyDescriptor.visitTargetSelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "SIMPLE_ENTITY" ) );
					assertThat( selection.getSelectionExpression(), is( "id" ) );
				}
		);

	}
}
