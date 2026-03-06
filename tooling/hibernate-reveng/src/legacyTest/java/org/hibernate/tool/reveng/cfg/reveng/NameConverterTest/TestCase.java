/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.cfg.reveng.NameConverterTest;

import org.hibernate.tool.reveng.internal.util.NameConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCase {

	@Test
	public void testSimplePluralizeWithSingleH() throws Exception {
		String plural = NameConverter.simplePluralize("h");
		assertEquals("hs", plural);
	}

	@Test
	public void testPluralize(){
		assertEquals("boxes", NameConverter.simplePluralize("box"));
		assertEquals("buses", NameConverter.simplePluralize("bus"));
		assertEquals("keys", NameConverter.simplePluralize("key"));
		assertEquals("countries", NameConverter.simplePluralize("country"));
		assertEquals("churches", NameConverter.simplePluralize("church"));
		assertEquals("bushes", NameConverter.simplePluralize("bush"));
		assertEquals("roofs", NameConverter.simplePluralize("roof"));
	}

}
