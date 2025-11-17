/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection;

import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.orm.test.envers.entities.collection.StringListEntity;
import org.hibernate.orm.test.envers.entities.collection.StringMapEntity;
import org.hibernate.orm.test.envers.entities.collection.StringSetEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-11901")
@EnversTest
@Jpa(annotatedClasses = { StringMapEntity.class, StringListEntity.class, StringSetEntity.class })
public class CollectionNullValueTest {
	private Integer mapId;
	private Integer listId;
	private Integer setId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Persist map with null values
		mapId = scope.fromTransaction( entityManager -> {
			final StringMapEntity sme = new StringMapEntity();
			sme.getStrings().put( "A", "B" );
			sme.getStrings().put( "B", null );
			entityManager.persist( sme );

			return sme.getId();
		} );

		// Update map with null values
		scope.inTransaction( entityManager -> {
			final StringMapEntity sme = entityManager.find( StringMapEntity.class, mapId );
			sme.getStrings().put( "C", null );
			sme.getStrings().put( "D", "E" );
			sme.getStrings().remove( "A" );
			entityManager.merge( sme );
		} );

		// Persist list with null values
		listId = scope.fromTransaction( entityManager -> {
			final StringListEntity sle = new StringListEntity();
			sle.getStrings().add( "A" );
			sle.getStrings().add( null );
			entityManager.persist( sle );

			return sle.getId();
		} );

		// Update list with null values
		scope.inTransaction( entityManager -> {
			final StringListEntity sle = entityManager.find( StringListEntity.class, listId );
			sle.getStrings().add( null );
			sle.getStrings().add( "D" );
			sle.getStrings().remove( "A" );
			entityManager.merge( sle );
		} );

		// Persist set with null values
		setId = scope.fromTransaction( entityManager -> {
			final StringSetEntity sse = new StringSetEntity();
			sse.getStrings().add( "A" );
			sse.getStrings().add( null );
			entityManager.persist( sse );

			return sse.getId();
		} );

		// Update set with null values
		scope.inTransaction( entityManager -> {
			final StringSetEntity sse = entityManager.find( StringSetEntity.class, setId );
			sse.getStrings().add( null );
			sse.getStrings().add( "D" );
			sse.getStrings().remove( "A" );
			entityManager.merge( sse );
		} );
	}

	@Test
	public void testStringMapHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final var auditReader = AuditReaderFactory.get( entityManager );
			final List<Number> revisions = auditReader.getRevisions( StringMapEntity.class, mapId );
			assertEquals( Arrays.asList( 1, 2 ), revisions );

			final StringMapEntity rev1 = auditReader.find( StringMapEntity.class, mapId, 1 );
			assertEquals( TestTools.makeMap( "A", "B" ), rev1.getStrings() );

			final StringMapEntity rev2 = auditReader.find( StringMapEntity.class, mapId, 2 );
			assertEquals( TestTools.makeMap( "D", "E" ), rev2.getStrings() );
		} );
	}

	@Test
	public void testStringListHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final var auditReader = AuditReaderFactory.get( entityManager );
			final List<Number> revisions = auditReader.getRevisions( StringListEntity.class, listId );
			assertEquals( Arrays.asList( 3, 4 ), revisions );

			final StringListEntity rev3 = auditReader.find( StringListEntity.class, listId, 3 );
			assertEquals( TestTools.makeList( "A" ), rev3.getStrings() );

			// NOTE: the only reason this assertion expects a null element is because the collection is indexed.
			// ORM will return a list that consists of { null, "D" } and Envers should effectively mimic that.
			final StringListEntity rev4 = auditReader.find( StringListEntity.class, listId, 4 );
			assertEquals( TestTools.makeList( null, "D" ), rev4.getStrings() );
		} );
	}

	@Test
	public void testStringSetHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			final var auditReader = AuditReaderFactory.get( entityManager );
			final List<Number> revisions = auditReader.getRevisions( StringSetEntity.class, setId );
			assertEquals( Arrays.asList( 5, 6 ), revisions );

			final StringSetEntity rev5 = auditReader.find( StringSetEntity.class, setId, 5 );
			assertEquals( TestTools.makeSet( "A" ), rev5.getStrings() );

			final StringSetEntity rev6 = auditReader.find( StringSetEntity.class, setId, 6 );
			assertEquals( TestTools.makeSet( "D" ), rev6.getStrings() );
		} );
	}
}
