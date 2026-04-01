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
	public void testConstructorAndKinds() {
		HQLCompletionProposal proposal = new HQLCompletionProposal(HQLCompletionProposal.ENTITY_NAME, 10);
		assertEquals(HQLCompletionProposal.ENTITY_NAME, proposal.getCompletionKind());
		assertEquals(10, proposal.getCompletionLocation());
	}

	@Test
	public void testKindMethods() {
		HQLCompletionProposal proposal = new HQLCompletionProposal(HQLCompletionProposal.KEYWORD, 0);
		assertEquals(HQLCompletionProposal.ALIAS_REF, proposal.aliasRefKind());
		assertEquals(HQLCompletionProposal.ENTITY_NAME, proposal.entityNameKind());
		assertEquals(HQLCompletionProposal.PROPERTY, proposal.propertyKind());
		assertEquals(HQLCompletionProposal.KEYWORD, proposal.keywordKind());
		assertEquals(HQLCompletionProposal.FUNCTION, proposal.functionKind());
	}

	@Test
	public void testSettersAndGetters() {
		HQLCompletionProposal proposal = new HQLCompletionProposal(HQLCompletionProposal.PROPERTY, 5);
		proposal.setCompletion("name");
		proposal.setSimpleName("name");
		proposal.setEntityName("org.example.Product");
		proposal.setShortEntityName("Product");
		proposal.setPropertyName("name");
		proposal.setRelevance(100);
		proposal.setReplaceStart(3);
		proposal.setReplaceEnd(5);

		assertEquals("name", proposal.getCompletion());
		assertEquals("name", proposal.getSimpleName());
		assertEquals("org.example.Product", proposal.getEntityName());
		assertEquals("Product", proposal.getShortEntityName());
		assertEquals("name", proposal.getPropertyName());
		assertEquals(100, proposal.getRelevance());
		assertEquals(3, proposal.getReplaceStart());
		assertEquals(5, proposal.getReplaceEnd());
		assertNull(proposal.getProperty());
	}

	@Test
	public void testToStringEntityName() {
		HQLCompletionProposal proposal = new HQLCompletionProposal(HQLCompletionProposal.ENTITY_NAME, 0);
		proposal.setCompletion("Product");
		proposal.setSimpleName("Product");
		String str = proposal.toString();
		assertTrue(str.contains("ENTITY_NAME"));
		assertTrue(str.contains("Product"));
	}

	@Test
	public void testToStringProperty() {
		HQLCompletionProposal proposal = new HQLCompletionProposal(HQLCompletionProposal.PROPERTY, 0);
		assertTrue(proposal.toString().contains("PROPERTY"));
	}

	@Test
	public void testToStringKeyword() {
		HQLCompletionProposal proposal = new HQLCompletionProposal(HQLCompletionProposal.KEYWORD, 0);
		assertTrue(proposal.toString().contains("KEYWORD"));
	}

	@Test
	public void testToStringUnknownKind() {
		HQLCompletionProposal proposal = new HQLCompletionProposal(99, 0);
		assertTrue(proposal.toString().contains("Unknown type"));
	}
}
