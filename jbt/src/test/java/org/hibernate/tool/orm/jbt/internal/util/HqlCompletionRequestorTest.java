/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
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
package org.hibernate.tool.orm.jbt.internal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.hibernate.tool.reveng.ide.completion.HQLCompletionProposal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HqlCompletionRequestorTest {
	
	private HqlCompletionRequestor hqlCompletionRequestor = null;
	
	private Object acceptedObject = null;
	private String message = null;
	
	@BeforeEach
	public void beforeEach() {
		hqlCompletionRequestor = new HqlCompletionRequestor(new Object() {
			@SuppressWarnings("unused")
			public boolean accept(Object o) { acceptedObject = o; return true; }
			@SuppressWarnings("unused")
			public void completionFailure(String s) { message = s; }
		});
	}
	
	@Test
	public void testConstruction() {
		assertNotNull(hqlCompletionRequestor);
		assertNull(acceptedObject);
		assertNull(message);
	}
	
	@Test
	public void testAccept() {
		HQLCompletionProposal objectToAccept = new HQLCompletionProposal(0, 0);
		assertNull(acceptedObject);
		hqlCompletionRequestor.accept(objectToAccept);
		assertNotNull(acceptedObject);
		assertSame(objectToAccept, acceptedObject);
	}
	
	@Test
	public void testCompletionFailure() {
		assertNull(message);
		hqlCompletionRequestor.completionFailure("foobar");
		assertNotNull(message);
		assertEquals("foobar", message);
	}
	
}
