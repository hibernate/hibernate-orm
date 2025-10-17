/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.model.source;

import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume Smet
 */
public class AttributePathTest {

	@Test
	@JiraKey(value = "HHH-10863")
	public void testCollectionElement() {
		AttributePath attributePath = AttributePath.parse( "items.{element}.name" );

		Assertions.assertFalse( attributePath.isCollectionElement() );
		Assertions.assertTrue( attributePath.getParent().isCollectionElement() );
		Assertions.assertFalse( attributePath.getParent().getParent().isCollectionElement() );
	}

}
