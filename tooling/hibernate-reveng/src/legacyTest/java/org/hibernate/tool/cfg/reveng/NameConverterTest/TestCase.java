/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hibernate.tool.cfg.reveng.NameConverterTest;

import org.hibernate.tool.internal.util.NameConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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