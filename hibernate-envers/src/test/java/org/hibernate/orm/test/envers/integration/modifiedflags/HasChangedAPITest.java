/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.integration.auditReader.AuditedTestEntity;
import org.hibernate.orm.test.envers.integration.auditReader.NotAuditedTestEntity;
import org.hibernate.orm.test.envers.integration.modifiedflags.entities.EnumEntity;
import org.hibernate.orm.test.envers.integration.modifiedflags.entities.EnumOption;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A test which checks the correct behavior of AuditReader.isEntityClassAudited(Class entityClass).
 *
 * @author Hernan Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@Jpa(integrationSettings = @Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"),
		annotatedClasses = {AuditedTestEntity.class, NotAuditedTestEntity.class, EnumEntity.class})
public class HasChangedAPITest extends AbstractModifiedFlagsEntityTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			AuditedTestEntity ent1 = new AuditedTestEntity( 1, "str1" );
			NotAuditedTestEntity ent2 = new NotAuditedTestEntity( 1, "str1" );
			EnumEntity ent3 = new EnumEntity( 1, EnumOption.A );


			em.persist( ent1 );
			em.persist( ent2 );
			em.persist( ent3 );
			em.getTransaction().commit();

			em.getTransaction().begin();

			ent1 = em.find( AuditedTestEntity.class, 1 );
			ent2 = em.find( NotAuditedTestEntity.class, 1 );
			ent3 = em.find( EnumEntity.class, 1 );
			ent1.setStr1( "str2" );
			ent2.setStr1( "str2" );
			ent3.setOption( EnumOption.B );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testHasChangedHasNotChangedCriteria(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			var list = auditReader.createQuery().forRevisionsOfEntity( AuditedTestEntity.class, true, true )
					.add( AuditEntity.property( "str1" ).hasChanged() ).getResultList();
			assertEquals( 2, list.size() );
			assertEquals( "str1", ((AuditedTestEntity) list.get( 0 )).getStr1() );
			assertEquals( "str2", ((AuditedTestEntity) list.get( 1 )).getStr1() );

			list = auditReader.createQuery().forRevisionsOfEntity( AuditedTestEntity.class, true, true )
					.add( AuditEntity.property( "str1" ).hasNotChanged() ).getResultList();
			assertTrue( list.isEmpty() );
		} );

	}

	@Test
	@JiraKey(value = "HHH-13770")
	public void testHasChangedHasNotChangedEnum(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			var list = auditReader.createQuery().forRevisionsOfEntity( EnumEntity.class, true, true )
					.add( AuditEntity.property( "option" ).hasChanged() ).getResultList();
			assertEquals( 2, list.size() );
			assertEquals( EnumOption.A, ((EnumEntity) list.get( 0 )).getOption() );
			assertEquals( EnumOption.B, ((EnumEntity) list.get( 1 )).getOption() );

			list = auditReader.createQuery().forRevisionsOfEntity( EnumEntity.class, true, true )
					.add( AuditEntity.property( "option" ).hasNotChanged() ).getResultList();
			assertTrue( list.isEmpty() );
		} );
	}

}
