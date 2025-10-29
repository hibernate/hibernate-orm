/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined.primarykeyjoin;

import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.orm.test.envers.integration.inheritance.joined.ParentEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@DomainModel(annotatedClasses = {ChildPrimaryKeyJoinEntity.class, ParentEntity.class})
@SessionFactory
public class ChildPrimaryKeyJoinAuditing {
	private Integer id1;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		id1 = 1;

		// Rev 1
		scope.inTransaction( session -> {
			ChildPrimaryKeyJoinEntity ce = new ChildPrimaryKeyJoinEntity( id1, "x", 1l );
			session.persist( ce );
		} );

		// Rev 2
		scope.inTransaction( session -> {
			ChildPrimaryKeyJoinEntity ce = session.find( ChildPrimaryKeyJoinEntity.class, id1 );
			ce.setData( "y" );
			ce.setNumVal( 2l );
		} );
	}

	@Test
	public void testRevisionsCounts(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertEquals( Arrays.asList( 1, 2 ), AuditReaderFactory.get( session ).getRevisions( ChildPrimaryKeyJoinEntity.class, id1 ) );
		} );
	}

	@Test
	public void testHistoryOfChildId1(SessionFactoryScope scope) {
		ChildPrimaryKeyJoinEntity ver1 = new ChildPrimaryKeyJoinEntity( id1, "x", 1l );
		ChildPrimaryKeyJoinEntity ver2 = new ChildPrimaryKeyJoinEntity( id1, "y", 2l );

		scope.inSession( session -> {
			final var auditReader = AuditReaderFactory.get( session );
			assertEquals( ver1, auditReader.find( ChildPrimaryKeyJoinEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( ChildPrimaryKeyJoinEntity.class, id1, 2 ) );

			assertEquals( ver1, auditReader.find( ParentEntity.class, id1, 1 ) );
			assertEquals( ver2, auditReader.find( ParentEntity.class, id1, 2 ) );
		} );
	}

	@Test
	public void testPolymorphicQuery(SessionFactoryScope scope) {
		ChildPrimaryKeyJoinEntity childVer1 = new ChildPrimaryKeyJoinEntity( id1, "x", 1l );

		scope.inSession( session -> {
			final var auditReader = AuditReaderFactory.get( session );
			assertEquals( childVer1, auditReader.createQuery()
					.forEntitiesAtRevision( ChildPrimaryKeyJoinEntity.class, 1 )
					.getSingleResult() );

			assertEquals( childVer1, auditReader.createQuery().forEntitiesAtRevision( ParentEntity.class, 1 ).getSingleResult() );
		} );
	}

	@Test
	public void testChildIdColumnName(DomainModelScope scope) {
		// Hibernate now sorts columns that are part of the key and therefore this test needs to test
		// for the existence of the specific key column rather than the expectation that is exists at
		// a specific order in the iterator.
		final PersistentClass persistentClass = scope.getDomainModel().getEntityBinding( ChildPrimaryKeyJoinEntity.class.getName() + "_AUD" );
		Assertions.assertNotNull( getColumnFromIteratorByName( persistentClass.getKey().getSelectables(), "other_id" ) );
	}

	private static Column getColumnFromIteratorByName(List<Selectable> selectables, String columnName) {
		for ( Selectable s : selectables ) {
			Column column = (Column) s;
			if ( column.getName().equals( columnName) ) {
				return column;
			}
		}
		return null;
	}
}
