/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchstrategyhelper;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.graph.FetchOptions;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
public class FetchStrategyDeterminationTests extends BaseCoreFunctionalTestCase {

	@Test
	public void testManyToOneDefaultFetch() {
		final EntityPersister entityDescriptor = sessionFactory().getMappingMetamodel().getEntityDescriptor( AnEntity.class );
		final AttributeMapping attributeMapping = entityDescriptor.findAttributeMapping( "otherEntityDefault" );
		final FetchOptions mappedFetchOptions = attributeMapping.getMappedFetchOptions();
		assertEquals( mappedFetchOptions.getTiming(), FetchTiming.IMMEDIATE );
		assertEquals( mappedFetchOptions.getStyle(), FetchStyle.JOIN );
	}

	@Test
	public void testManyToOneJoinFetch() {
		final EntityPersister entityDescriptor = sessionFactory().getMappingMetamodel().getEntityDescriptor( AnEntity.class );
		final AttributeMapping attributeMapping = entityDescriptor.findAttributeMapping( "otherEntityJoin" );
		final FetchOptions mappedFetchOptions = attributeMapping.getMappedFetchOptions();
		assertEquals( mappedFetchOptions.getTiming(), FetchTiming.IMMEDIATE );
		assertEquals( mappedFetchOptions.getStyle(), FetchStyle.JOIN );
	}

	@Test
	public void testManyToOneSelectFetch() {
		final EntityPersister entityDescriptor = sessionFactory().getMappingMetamodel().getEntityDescriptor( AnEntity.class );
		final AttributeMapping attributeMapping = entityDescriptor.findAttributeMapping( "otherEntitySelect" );
		final FetchOptions mappedFetchOptions = attributeMapping.getMappedFetchOptions();
		assertEquals( mappedFetchOptions.getTiming(), FetchTiming.IMMEDIATE );
		assertEquals( mappedFetchOptions.getStyle(), FetchStyle.SELECT );
	}
//
//	private org.hibernate.FetchMode determineFetchMode(Class<?> entityClass, String path) {
//		AbstractEntityPersister entityPersister = (AbstractEntityPersister)
//				sessionFactory().getMappingMetamodel().getEntityDescriptor(entityClass.getName());
//		int index = entityPersister.getPropertyIndex( path );
//		return  entityPersister.getFetchMode( index );
//	}
//
//	private AssociationType determineAssociationType(Class<?> entityClass, String path) {
//		AbstractEntityPersister entityPersister = (AbstractEntityPersister)
//				sessionFactory().getMappingMetamodel().getEntityDescriptor(entityClass.getName());
//		int index = entityPersister.getPropertyIndex( path );
//		return (AssociationType) entityPersister.getSubclassPropertyType( index );
//	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class,
				OtherEntity.class
		};
	}
	@Entity
	@Table(name="entity")
	public static class AnEntity {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private OtherEntity otherEntityDefault;

		@ManyToOne
		@Fetch(FetchMode.JOIN)
		private OtherEntity otherEntityJoin;

		@ManyToOne
		@Fetch(FetchMode.SELECT)
		private OtherEntity otherEntitySelect;

		// @Fetch(FetchMode.SUBSELECT) is not allowed for ToOne associations
	}

	@Entity
	@Table(name="otherentity")
	public static class OtherEntity {
		@Id
		@GeneratedValue
		private Long id;
	}
}
