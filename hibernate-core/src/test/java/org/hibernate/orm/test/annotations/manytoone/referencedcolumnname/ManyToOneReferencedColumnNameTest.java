/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone.referencedcolumnname;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialectFeature(feature= DialectFeatureChecks.SupportsIdentityColumns.class)
@ServiceRegistry(settings = @Setting(name= MappingSettings.IMPLICIT_NAMING_STRATEGY, value = "legacy-jpa"))
@DomainModel(annotatedClasses = {
		Item.class,
		Vendor.class,
		WarehouseItem.class,
		ZItemCost.class
})
@SessionFactory
public class ManyToOneReferencedColumnNameTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testRecoverableExceptionInFkOrdering(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var v = new Vendor();
			var i = new Item();
			var ic = new ZItemCost();
			ic.setCost( new BigDecimal( 2 ) );
			ic.setItem( i );
			ic.setVendor( v );
			var wi = new WarehouseItem();
			wi.setDefaultCost( ic );
			wi.setItem( i );
			wi.setVendor( v );
			wi.setQtyInStock( new BigDecimal( 2 ) );

			session.persist( i );
			session.persist( v );
			session.persist( ic );
			session.persist( wi );
		} );
	}
}
