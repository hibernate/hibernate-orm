/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/mapping/generated/ComponentOwner.hbm.xml"
)
@SessionFactory
@RequiresDialect( value = OracleDialect.class )
public class PartiallyGeneratedComponentTest {

	@Test
	public void testPartialComponentGeneration(SessionFactoryScope scope) {
		ComponentOwner owner = new ComponentOwner( "initial" );
		scope.inTransaction(
				s -> s.persist( owner )
		);

		assertNotNull( owner.getComponent(), "expecting insert value generation" );
		int previousValue = owner.getComponent().getGenerated();
		assertFalse( 0 == previousValue, "expecting insert value generation" );

		ComponentOwner owner2 = scope.fromTransaction(
				s -> {
					ComponentOwner _owner = s.get( ComponentOwner.class, owner.getId() );
					assertEquals( previousValue, _owner.getComponent().getGenerated(), "expecting insert value generation" );
					_owner.setName( "subsequent" );
					return _owner;
				}
		);

		assertNotNull( owner2.getComponent() );
		int previousValue2 = owner2.getComponent().getGenerated();

		scope.inTransaction(
				s -> {
					ComponentOwner _owner = s.get( ComponentOwner.class, owner.getId() );
					assertEquals( previousValue2, _owner.getComponent().getGenerated(), "expecting update value generation" );
					s.remove( _owner );
				}
		);
	}
}
