/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone.hhh4851;

import org.hibernate.PropertyValueException;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Fail.fail;


/**
 * @author Emmanuel Bernard
 */
@JiraKey("HHH-4851")
@DomainModel(
		annotatedClasses = {
				Hardware.class,
				DeviceGroupConfig.class,
				Hardware.class,
				ManagedDevice.class,
				Device.class,
				Owner.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = Environment.CHECK_NULLABILITY, value = "true")
)
public class HHH4851Test {

	@Test
	public void testHHH4851(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Owner org = new Owner();
					org.setName( "root" );
					session.persist( org );

					ManagedDevice lTerminal = new ManagedDevice();
					lTerminal.setName( "test" );
					lTerminal.setOwner( org );
					session.persist( lTerminal );

					Device terminal = new Device();
					terminal.setTag( "test" );
					terminal.setOwner( org );
					try {
						session.merge( terminal );
					}
					catch (PropertyValueException e) {
						fail( "not-null checking should not be raised: " + e.getMessage() );
					}
				}
		);
	}

}
