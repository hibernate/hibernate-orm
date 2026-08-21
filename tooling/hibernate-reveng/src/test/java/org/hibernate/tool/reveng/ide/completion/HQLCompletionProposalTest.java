/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.ide.completion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HQLCompletionProposalTest {

	@Test
	public void testConstructorDefaults() {
		HQLCompletionProposal proposal = new HQLCompletionProposal(HQLCompletionProposal.ENTITY_NAME, 10);
		assertEquals(HQLCompletionProposal.ENTITY_NAME, proposal.getCompletionKind());
		assertEquals(10, proposal.getCompletionLocation());
		assertEquals("", proposal.getCompletion());
		assertEquals("", proposal.getSimpleName());
		assertEquals(0, proposal.getReplaceStart());
		assertEquals(0, proposal.getReplaceEnd());
		assertEquals(1, proposal.getRelevance());
		assertNull(proposal.getEntityName());
		assertNull(proposal.getShortEntityName());
		assertNull(proposal.getPropertyName());
		assertNull(proposal.getProperty());
	}

	@Test
	public void testSettersAndGetters() {
		HQLCompletionProposal proposal = new HQLCompletionProposal(HQLCompletionProposal.KEYWORD, 0);
		proposal.setCompletion("SELECT");
		proposal.setSimpleName("select");
		proposal.setReplaceStart(5);
		proposal.setReplaceEnd(11);
		proposal.setRelevance(100);
		proposal.setEntityName("com.example.Person");
		proposal.setShortEntityName("Person");
		proposal.setPropertyName("name");
		proposal.setCompletionKind(HQLCompletionProposal.PROPERTY);
		proposal.setCompletionLocation(20);

		assertEquals("SELECT", proposal.getCompletion());
		assertEquals("select", proposal.getSimpleName());
		assertEquals(5, proposal.getReplaceStart());
		assertEquals(11, proposal.getReplaceEnd());
		assertEquals(100, proposal.getRelevance());
		assertEquals("com.example.Person", proposal.getEntityName());
		assertEquals("Person", proposal.getShortEntityName());
		assertEquals("name", proposal.getPropertyName());
		assertEquals(HQLCompletionProposal.PROPERTY, proposal.getCompletionKind());
		assertEquals(20, proposal.getCompletionLocation());
	}

	@Test
	public void testToStringEntityName() {
		HQLCompletionProposal proposal = new HQLCompletionProposal(HQLCompletionProposal.ENTITY_NAME, 5);
		proposal.setCompletion("Person");
		proposal.setSimpleName("Person");
		String s = proposal.toString();
		assertTrue(s.contains("ENTITY_NAME"));
		assertTrue(s.contains("Person"));
	}

	@Test
	public void testToStringProperty() {
		HQLCompletionProposal proposal = new HQLCompletionProposal(HQLCompletionProposal.PROPERTY, 5);
		String s = proposal.toString();
		assertTrue(s.contains("PROPERTY"));
	}

	@Test
	public void testToStringKeyword() {
		HQLCompletionProposal proposal = new HQLCompletionProposal(HQLCompletionProposal.KEYWORD, 5);
		String s = proposal.toString();
		assertTrue(s.contains("KEYWORD"));
	}

	@Test
	public void testToStringUnknownKind() {
		HQLCompletionProposal proposal = new HQLCompletionProposal(99, 5);
		String s = proposal.toString();
		assertTrue(s.contains("<Unknown type>"));
	}

	@Test
	public void testKindMethods() {
		HQLCompletionProposal proposal = new HQLCompletionProposal(HQLCompletionProposal.ENTITY_NAME, 0);
		assertEquals(HQLCompletionProposal.ALIAS_REF, proposal.aliasRefKind());
		assertEquals(HQLCompletionProposal.ENTITY_NAME, proposal.entityNameKind());
		assertEquals(HQLCompletionProposal.PROPERTY, proposal.propertyKind());
		assertEquals(HQLCompletionProposal.KEYWORD, proposal.keywordKind());
		assertEquals(HQLCompletionProposal.FUNCTION, proposal.functionKind());
	}

	@Test
	public void testConstants() {
		assertEquals(1, HQLCompletionProposal.ENTITY_NAME);
		assertEquals(2, HQLCompletionProposal.PROPERTY);
		assertEquals(3, HQLCompletionProposal.KEYWORD);
		assertEquals(4, HQLCompletionProposal.FUNCTION);
		assertEquals(5, HQLCompletionProposal.ALIAS_REF);
	}
}
