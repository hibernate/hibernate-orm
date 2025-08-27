/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.model.source;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
public class AttributePathTest {

	@Test
	@JiraKey(value = "HHH-10863")
	public void testCollectionElement() {
		AttributePath attributePath = AttributePath.parse( "items.{element}.name" );

		assertFalse( attributePath.isCollectionElement() );
		assertTrue( attributePath.getParent().isCollectionElement() );
		assertFalse( attributePath.getParent().getParent().isCollectionElement() );
	}

}
