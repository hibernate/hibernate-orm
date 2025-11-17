/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.integration.basic.BasicTestEntity1;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@DomainModel(annotatedClasses = {BasicTestEntity1.class})
@ServiceRegistry(settings = {
		@Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"),
		@Setting(name = EnversSettings.MODIFIED_FLAG_SUFFIX, value = "_CHANGED")
})
@SessionFactory
public class ModifiedFlagSuffix extends AbstractModifiedFlagsEntityTest {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		scope.inSession( em -> {
			em.getTransaction().begin();
			BasicTestEntity1 bte1 = new BasicTestEntity1( "x", 1 );
			em.persist( bte1 );
			em.getTransaction().commit();

			id1 = bte1.getId();
		} );
	}

	@Test
	public void testModFlagProperties(DomainModelScope scope) {
		assertEquals(
				TestTools.makeSet( "str1_CHANGED", "long1_CHANGED" ),
				TestTools.extractModProperties(
						scope.getDomainModel().getEntityBinding( BasicTestEntity1.class.getName() + "_AUD" ),
						"_CHANGED"
				)
		);
	}

	@Test
	public void testHasChanged(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChangedWithDeleted(
					auditReader,
					BasicTestEntity1.class,
					id1, "str1"
			);
			assertEquals( 1, list.size() );
			assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasChangedWithDeleted(
					auditReader,
					BasicTestEntity1.class,
					id1, "long1"
			);
			assertEquals( 1, list.size() );
			assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );

			list = auditReader.createQuery().forRevisionsOfEntity( BasicTestEntity1.class, false, true )
					.add( AuditEntity.property( "str1" ).hasChanged() )
					.add( AuditEntity.property( "long1" ).hasChanged() )
					.getResultList();
			assertEquals( 1, list.size() );
			assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );
		} );
	}
}
