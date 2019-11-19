/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping.onetoone;

import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.SingularAssociationAttributeMapping;
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
				.getMetamodel()
				.findEntityDescriptor( EntityWithOneToOneJoinTable.class );
		final ModelPart other = entityWithOneToOneJoinTableDescriptor.findSubPart( "other" );
		assertThat( other, instanceOf( SingularAssociationAttributeMapping.class ) );

		final SingularAssociationAttributeMapping otherAttributeMapping = (SingularAssociationAttributeMapping) other;

		final ForeignKeyDescriptor foreignKeyDescriptor = otherAttributeMapping.getForeignKeyDescriptor();
		foreignKeyDescriptor.visitReferringColumns( (keyTable, keyColumn, jdbcMapping) -> {
			assertThat( keyTable, is( "Entity_SimpleEntity" ) );
			assertThat( keyColumn, is( "other_id" ) );
		} );

		foreignKeyDescriptor.visitTargetColumns( (targetTable, targetColumn, jdbcMapping) -> {
			assertThat( targetTable, is( "SIMPLE_ENTITY" ) );
			assertThat( targetColumn, is( "id" ) );
		} );
	}
}
