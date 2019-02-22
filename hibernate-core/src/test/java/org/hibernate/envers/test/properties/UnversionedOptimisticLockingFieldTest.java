/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.properties;

import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.properties.UnversionedOptimisticLockingFieldEntity;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 * @author Nicolas Doroskevich
 */
public class UnversionedOptimisticLockingFieldTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { UnversionedOptimisticLockingFieldEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.DO_NOT_AUDIT_OPTIMISTIC_LOCKING_FIELD, "true" );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				entityManager -> {
					UnversionedOptimisticLockingFieldEntity entity = new UnversionedOptimisticLockingFieldEntity( "x" );
					entityManager.persist( entity );
					id1 = entity.getId();
				},
				entityManager -> {
					UnversionedOptimisticLockingFieldEntity entity = entityManager.find(
							UnversionedOptimisticLockingFieldEntity.class,
							id1
					);
					entity.setStr( "y" );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( UnversionedOptimisticLockingFieldEntity.class, id1 ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		UnversionedOptimisticLockingFieldEntity ver1 = new UnversionedOptimisticLockingFieldEntity( id1, "x" );
		UnversionedOptimisticLockingFieldEntity ver2 = new UnversionedOptimisticLockingFieldEntity( id1, "y" );

		assertThat( getAuditReader().find( UnversionedOptimisticLockingFieldEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( UnversionedOptimisticLockingFieldEntity.class, id1, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testMapping() {
		final EntityTypeDescriptor<?> entityDescriptor = entityManagerFactoryScope().getEntityManagerFactory()
				.unwrap( SessionFactoryImplementor.class )
				.getMetamodel()
				.entity( UnversionedOptimisticLockingFieldEntity.class.getName() + "_AUD" );

		for ( NonIdPersistentAttribute attribute : entityDescriptor.getPersistentAttributes() ) {
			System.out.println( attribute.getName() );
			assertThat( attribute.getName(), not( equalTo( "optLocking" ) ) );
		}
	}
}
