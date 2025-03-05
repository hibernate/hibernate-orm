/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.domain.gambit.EntityWithOneToOneJoinTable;
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
				EntityWithOneToOneJoinTable.class,
				SimpleEntity.class
		}
)
@ServiceRegistry
@SessionFactory
public class EntityWithOneToOneJoinTableTest {

	@Test
	public void basicTest(SessionFactoryScope scope) {
		final EntityPersister entityWithOneToOneJoinTableDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.findEntityDescriptor( EntityWithOneToOneJoinTable.class );
		final ModelPart other = entityWithOneToOneJoinTableDescriptor.findSubPart( "other" );
		assertThat( other, instanceOf( ToOneAttributeMapping.class ) );

		final ToOneAttributeMapping otherAttributeMapping = (ToOneAttributeMapping) other;

		final ForeignKeyDescriptor foreignKeyDescriptor = otherAttributeMapping.getForeignKeyDescriptor();
		foreignKeyDescriptor.visitKeySelectables(
				(columnIndex, selection) -> {
					assertThat( selection.getContainingTableExpression(), is( "Entity_SimpleEntity" ) );
					assertThat( selection.getSelectionExpression(), is( "other_id" ) );
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
