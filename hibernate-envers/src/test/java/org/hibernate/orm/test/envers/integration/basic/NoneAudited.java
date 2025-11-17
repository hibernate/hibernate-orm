/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import java.util.List;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.junit.jupiter.api.Test;

import static org.hibernate.envers.internal.tools.Tools.collectionToList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@EnversTest
@DomainModel(annotatedClasses = {BasicTestEntity3.class})
@SessionFactory
public class NoneAudited {
	@Test
	public void testRevisionInfoTableNotCreated(DomainModelScope scope) {
		@SuppressWarnings("unchecked")
		List<PersistentClass> pcs = collectionToList( scope.getDomainModel().getEntityBindings() );
		assertEquals( 1, pcs.size() );
		assertTrue( pcs.get( 0 ).getClassName().contains( "BasicTestEntity3" ) );
	}
}
