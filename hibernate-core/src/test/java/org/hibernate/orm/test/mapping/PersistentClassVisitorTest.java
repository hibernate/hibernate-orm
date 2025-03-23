/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClassVisitor;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Simple smoke style tests to make sure visitors keep working.
 *
 * @author max
 */
@BaseUnitTest
public class PersistentClassVisitorTest {

	private StandardServiceRegistry serviceRegistry;
	private MetadataBuildingContext metadataBuildingContext;

	@BeforeEach
	public void prepare() {
		serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
	}

	@AfterEach
	public void release() {
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Test
	public void testProperCallbacks() {
		PersistentClassVisitorValidator vv = new PersistentClassVisitorValidator();
		new RootClass( metadataBuildingContext ).accept( vv );
		new Subclass( new RootClass( metadataBuildingContext ), metadataBuildingContext ).accept( vv );
		new JoinedSubclass( new RootClass( metadataBuildingContext ), metadataBuildingContext ).accept( vv );
		new SingleTableSubclass( new RootClass( metadataBuildingContext ), metadataBuildingContext ).accept( vv );
		new UnionSubclass( new RootClass( metadataBuildingContext ), metadataBuildingContext ).accept( vv );
	}

	static public class PersistentClassVisitorValidator implements PersistentClassVisitor {

		private Object validate(Class expectedClass, Object visitee) {
			if (!visitee.getClass().getName().equals(expectedClass.getName())) {
				throw new IllegalStateException(visitee.getClass().getName()
						+ " did not call proper accept method. Was "
						+ expectedClass.getName());
			}
			return null;
		}

		public Object accept(RootClass class1) {
			return validate(RootClass.class, class1);
		}

		public Object accept(UnionSubclass subclass) {
			return validate(UnionSubclass.class, subclass);
		}

		public Object accept(SingleTableSubclass subclass) {
			return validate(SingleTableSubclass.class, subclass);
		}

		public Object accept(JoinedSubclass subclass) {
			return validate(JoinedSubclass.class, subclass);
		}

		public Object accept(Subclass subclass) {
			return validate(Subclass.class, subclass);
		}
	}

}
