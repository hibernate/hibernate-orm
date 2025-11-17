/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache;

import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.support.DomainDataRegionTemplate;
import org.hibernate.orm.test.jcache.domain.Event;
import org.hibernate.orm.test.jcache.domain.Item;
import org.hibernate.orm.test.jcache.domain.VersionedItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;

/**
 * @author Steve Ebersole
 */
public class DomainDataRegionTest extends BaseFunctionalTest {
	@BeforeEach
	public void createSessionFactory() {
		TestHelper.createCache( "a.b.c" );
		super.createSessionFactory();
	}

	@Test
	public void testBasicUsage() {
		final Region region = sessionFactory().getCache().getRegion( TestHelper.entityRegionNames[0] );
		final DomainDataRegionTemplate domainDataRegion = assertTyping( DomainDataRegionTemplate.class, region );

		// see if we can get access to all of the access objects we think should be defined in this region

		final EntityDataAccess itemAccess = domainDataRegion.getEntityDataAccess(
				sessionFactory().getMappingMetamodel().getEntityDescriptor( Item.class ).getNavigableRole()
		);
		assertThat(
				itemAccess.getAccessType(),
				equalTo( AccessType.READ_WRITE )
		);

		assertThat(
				sessionFactory().getMappingMetamodel().getEntityDescriptor( VersionedItem.class ).getCacheAccessStrategy().getAccessType(),
				equalTo( AccessType.READ_WRITE )
		);

		assertThat(
				sessionFactory().getMappingMetamodel().getEntityDescriptor( Event.class ).getCacheAccessStrategy().getAccessType(),
				equalTo( AccessType.READ_WRITE )
		);

		assertThat(
				sessionFactory().getMappingMetamodel()
						.getCollectionDescriptor( Event.class.getName() + ".participants" )
						.getCacheAccessStrategy()
						.getAccessType(),
				equalTo( AccessType.READ_WRITE )
		);
	}
}
