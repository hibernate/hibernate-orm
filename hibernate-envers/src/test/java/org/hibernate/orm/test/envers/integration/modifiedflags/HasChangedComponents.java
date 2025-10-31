/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.components.Component1;
import org.hibernate.orm.test.envers.entities.components.Component2;
import org.hibernate.orm.test.envers.entities.components.ComponentTestEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.tools.TestTools.extractRevisionNumbers;
import static org.hibernate.orm.test.envers.tools.TestTools.makeList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@DomainModel(annotatedClasses = {ComponentTestEntity.class})
@ServiceRegistry(settings = @Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true"))
@SessionFactory
public class HasChangedComponents extends AbstractModifiedFlagsEntityTest {
	private Integer id1;
	private Integer id2;
	private Integer id3;
	private Integer id4;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		scope.inSession( em -> {
			// Revision 1
			em.getTransaction().begin();

			ComponentTestEntity cte1 = new ComponentTestEntity( new Component1( "a", "b" ), new Component2( "x", "y" ) );
			ComponentTestEntity cte2 = new ComponentTestEntity(
					new Component1( "a2", "b2" ), new Component2(
					"x2",
					"y2"
			)
			);
			ComponentTestEntity cte3 = new ComponentTestEntity(
					new Component1( "a3", "b3" ), new Component2(
					"x3",
					"y3"
			)
			);
			ComponentTestEntity cte4 = new ComponentTestEntity( null, null );

			em.persist( cte1 );
			em.persist( cte2 );
			em.persist( cte3 );
			em.persist( cte4 );

			em.getTransaction().commit();

			// Revision 2
			em.getTransaction().begin();

			ComponentTestEntity cte1Loaded = em.find( ComponentTestEntity.class, cte1.getId() );
			ComponentTestEntity cte2Loaded = em.find( ComponentTestEntity.class, cte2.getId() );
			ComponentTestEntity cte3Loaded = em.find( ComponentTestEntity.class, cte3.getId() );
			ComponentTestEntity cte4Loaded = em.find( ComponentTestEntity.class, cte4.getId() );

			cte1Loaded.setComp1( new Component1( "a'", "b'" ) );
			cte2Loaded.getComp1().setStr1( "a2'" );
			cte3Loaded.getComp2().setStr6( "y3'" );
			cte4Loaded.setComp1( new Component1() );
			cte4Loaded.getComp1().setStr1( "n" );
			cte4Loaded.setComp2( new Component2() );
			cte4Loaded.getComp2().setStr5( "m" );

			em.getTransaction().commit();

			// Revision 3
			em.getTransaction().begin();

			cte1Loaded = em.find( ComponentTestEntity.class, cte1.getId() );
			cte2Loaded = em.find( ComponentTestEntity.class, cte2.getId() );
			cte3Loaded = em.find( ComponentTestEntity.class, cte3.getId() );
			cte4Loaded = em.find( ComponentTestEntity.class, cte4.getId() );

			cte1Loaded.setComp2( new Component2( "x'", "y'" ) );
			cte3Loaded.getComp1().setStr2( "b3'" );
			cte4Loaded.setComp1( null );
			cte4Loaded.setComp2( null );

			em.getTransaction().commit();

			// Revision 4
			em.getTransaction().begin();

			cte2Loaded = em.find( ComponentTestEntity.class, cte2.getId() );

			em.remove( cte2Loaded );

			em.getTransaction().commit();

			id1 = cte1.getId();
			id2 = cte2.getId();
			id3 = cte3.getId();
			id4 = cte4.getId();
		} );
	}

	@Test
	public void testModFlagProperties(DomainModelScope scope) {
		assertEquals(
				TestTools.makeSet( "comp1_MOD" ),
				TestTools.extractModProperties(
						scope.getDomainModel().getEntityBinding(
								"org.hibernate.orm.test.envers.entities.components.ComponentTestEntity_AUD"
						)
				)
		);
	}

	@Test
	public void testHasChangedNotAudited(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertThrows( IllegalArgumentException.class, () ->
				queryForPropertyHasChanged( auditReader, ComponentTestEntity.class, id1, "comp2" )
			);
		} );
	}

	@Test
	public void testHasChangedId1(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChanged( auditReader, ComponentTestEntity.class, id1, "comp1" );
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 2 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChanged( auditReader, ComponentTestEntity.class, id1, "comp1" );
			assertEquals( 0, list.size() );
		} );
	}

	@Test
	public void testHasChangedId2(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChangedWithDeleted( auditReader, ComponentTestEntity.class, id2, "comp1" );
			assertEquals( 3, list.size() );
			assertEquals( makeList( 1, 2, 4 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChangedWithDeleted( auditReader, ComponentTestEntity.class, id2, "comp1" );
			assertEquals( 0, list.size() );
		} );
	}

	@Test
	public void testHasChangedId3(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChangedWithDeleted( auditReader, ComponentTestEntity.class, id3, "comp1" );
			assertEquals( 2, list.size() );
			assertEquals( makeList( 1, 3 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChangedWithDeleted( auditReader, ComponentTestEntity.class, id3, "comp1" );
			assertEquals( 0, list.size() );
		} );
	}

	@Test
	public void testHasChangedId4(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			List list = queryForPropertyHasChangedWithDeleted( auditReader, ComponentTestEntity.class, id4, "comp1" );
			assertEquals( 2, list.size() );
			assertEquals( makeList( 2, 3 ), extractRevisionNumbers( list ) );

			list = queryForPropertyHasNotChangedWithDeleted( auditReader, ComponentTestEntity.class, id4, "comp1" );
			assertEquals( 1, list.size() );
			assertEquals( makeList( 1 ), extractRevisionNumbers( list ) );
		} );
	}
}
