/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associationmanagement;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

@DomainModel(annotatedClasses = {
		AbstractBidirectionalityManagementSecondLevelCacheTest.CachedParent.class,
		AbstractBidirectionalityManagementSecondLevelCacheTest.CachedChild.class,
		AbstractBidirectionalityManagementSecondLevelCacheTest.CachedStudent.class,
		AbstractBidirectionalityManagementSecondLevelCacheTest.CachedCourse.class,
		AbstractBidirectionalityManagementSecondLevelCacheTest.CachedPerson.class,
		AbstractBidirectionalityManagementSecondLevelCacheTest.CachedPassport.class
})
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		@Setting(name = AvailableSettings.BIDIRECTIONALITY_MANAGEMENT, value = "true"),
		@Setting(name = AvailableSettings.AUTO_EVICT_COLLECTION_CACHE, value = "true")
})
@SessionFactory
class BidirectionalityManagementSecondLevelCacheWithAutoEvictionTest
		extends AbstractBidirectionalityManagementSecondLevelCacheTest {
}
