/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.single.discriminatorformula;

import java.util.Arrays;
import java.util.List;

import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.mapping.Formula;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@EnversTest
@DomainModel(annotatedClasses = { ClassTypeEntity.class, ParentEntity.class, ChildEntity.class })
@SessionFactory
public class DiscriminatorFormulaTest {
	private ChildEntity childVer1 = null;
	private ChildEntity childVer2 = null;
	private ParentEntity parentVer1 = null;
	private ParentEntity parentVer2 = null;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		// Child entity type
		final Long childTypeId = scope.fromTransaction( em -> {
			ClassTypeEntity childType = new ClassTypeEntity();
			childType.setType( ClassTypeEntity.CHILD_TYPE );
			em.persist( childType );
			return childType.getId();
		} );

		// Parent entity type
		final Long parentTypeId = scope.fromTransaction( em -> {
			ClassTypeEntity parentType = new ClassTypeEntity();
			parentType.setType( ClassTypeEntity.PARENT_TYPE );
			em.persist( parentType );
			return parentType.getId();
		} );

		// Child Rev 1
		final Long childId = scope.fromTransaction( em -> {
			ChildEntity child = new ChildEntity( childTypeId, "Child data", "Child specific data" );
			em.persist( child );
			return child.getId();
		} );

		// Parent Rev 2
		final Long parentId = scope.fromTransaction( em -> {
			ParentEntity parent = new ParentEntity( parentTypeId, "Parent data" );
			em.persist( parent );
			return parent.getId();
		} );

		// Child Rev 3
		scope.inTransaction( em -> {
			ChildEntity child = em.find( ChildEntity.class, childId );
			child.setData( "Child data modified" );
		} );

		// Parent Rev 4
		scope.inTransaction( em -> {
			ParentEntity parent = em.find( ParentEntity.class, parentId );
			parent.setData( "Parent data modified" );
		} );

		childVer1 = new ChildEntity( childId, childTypeId, "Child data", "Child specific data" );
		childVer2 = new ChildEntity( childId, childTypeId, "Child data modified", "Child specific data" );
		parentVer1 = new ParentEntity( parentId, parentTypeId, "Parent data" );
		parentVer2 = new ParentEntity( parentId, parentTypeId, "Parent data modified" );
	}

	@Test
	public void testDiscriminatorFormulaInAuditTable(DomainModelScope scope) {
		final var parentAudit = scope.getDomainModel().getEntityBinding(
				"org.hibernate.orm.test.envers.integration.inheritance.single.discriminatorformula.ParentEntity_AUD"
		);
		assertTrue( parentAudit.getDiscriminator().hasFormula() );
		boolean found = false;
		for ( Object o : parentAudit.getDiscriminator().getSelectables() ) {
			if ( o instanceof Formula formula ) {
				assertEquals( ParentEntity.DISCRIMINATOR_QUERY, formula.getText() );
				found = true;
				break;
			}
		}
		assertTrue( found );
	}

	@Test
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "'TYPE' is not escaped even though autoQuoteKeywords is enabled")
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					Arrays.asList( 1, 3 ),
					auditReader.getRevisions( ChildEntity.class, childVer1.getId() )
			);
			assertEquals(
					Arrays.asList( 2, 4 ),
					auditReader.getRevisions( ParentEntity.class, parentVer1.getId() )
			);
		} );
	}

	@Test
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "'TYPE' is not escaped even though autoQuoteKeywords is enabled")
	public void testHistoryOfParent(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( parentVer1, auditReader.find( ParentEntity.class, parentVer1.getId(), 2 ) );
			assertEquals( parentVer2, auditReader.find( ParentEntity.class, parentVer2.getId(), 4 ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "'TYPE' is not escaped even though autoQuoteKeywords is enabled")
	public void testHistoryOfChild(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( childVer1, auditReader.find( ChildEntity.class, childVer1.getId(), 1 ) );
			assertEquals( childVer2, auditReader.find( ChildEntity.class, childVer2.getId(), 3 ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "'TYPE' is not escaped even though autoQuoteKeywords is enabled")
	public void testPolymorphicQuery(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals(
					childVer1,
					auditReader.createQuery().forEntitiesAtRevision( ChildEntity.class, 1 ).getSingleResult()
			);
			assertEquals(
					childVer1,
					auditReader.createQuery().forEntitiesAtRevision( ParentEntity.class, 1 ).getSingleResult()
			);

			List childEntityRevisions = auditReader.createQuery().forRevisionsOfEntity(
					ChildEntity.class,
					true,
					false
			).getResultList();
			assertEquals( Arrays.asList( childVer1, childVer2 ), childEntityRevisions );

			List parentEntityRevisions = auditReader.createQuery().forRevisionsOfEntity(
					ParentEntity.class,
					true,
					false
			).getResultList();
			assertEquals( Arrays.asList( childVer1, parentVer1, childVer2, parentVer2 ), parentEntityRevisions );
		} );
	}
}
