/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.ide.completion;

import org.hibernate.boot.Metadata;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.test.utils.HibernateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HQLCodeAssistTest {

	private static final String[] HBM_XML_FILES = new String[] {
			"HelloWorld.hbm.xml"
	};

	@TempDir
	public File outputFolder;

	private HQLCodeAssist codeAssist;
	private Metadata metadata;

	@BeforeEach
	public void setUp() {
		MetadataDescriptor metadataDescriptor = HibernateUtil
				.initializeMetadataDescriptor(this, HBM_XML_FILES, outputFolder);
		metadata = metadataDescriptor.createMetadata();
		codeAssist = new HQLCodeAssist(metadata);
	}

	@Test
	public void testEntityNameCompletionAfterFrom() {
		List<HQLCompletionProposal> proposals = complete("from ");
		assertTrue(hasProposalOfKind(proposals, HQLCompletionProposal.ENTITY_NAME));
	}

	@Test
	public void testEntityNameCompletionWithPrefix() {
		List<HQLCompletionProposal> proposals = complete("from Hello");
		assertTrue(proposals.stream()
				.anyMatch(p -> p.getCompletionKind() == HQLCompletionProposal.ENTITY_NAME
						&& p.getSimpleName().startsWith("Hello")));
	}

	@Test
	public void testKeywordCompletion() {
		// Keywords are returned alongside other completions when not in a FROM context
		List<HQLCompletionProposal> proposals = complete("from HelloWorld as hw where hw.id > 1 or");
		assertTrue(hasProposalOfKind(proposals, HQLCompletionProposal.KEYWORD));
		assertTrue(proposals.stream()
				.anyMatch(p -> p.getCompletionKind() == HQLCompletionProposal.KEYWORD
						&& p.getSimpleName().equals("order")));
	}

	@Test
	public void testAliasCompletion() {
		List<HQLCompletionProposal> proposals = complete("from HelloWorld as hw where h");
		assertTrue(hasProposalOfKind(proposals, HQLCompletionProposal.ALIAS_REF));
		assertTrue(proposals.stream()
				.anyMatch(p -> p.getCompletionKind() == HQLCompletionProposal.ALIAS_REF
						&& p.getSimpleName().equals("hw")));
	}

	@Test
	public void testPropertyCompletion() {
		List<HQLCompletionProposal> proposals = complete("from HelloWorld as hw where hw.");
		assertTrue(hasProposalOfKind(proposals, HQLCompletionProposal.PROPERTY));
	}

	@Test
	public void testFunctionCompletion() {
		List<HQLCompletionProposal> proposals = complete("from HelloWorld where sub");
		assertTrue(proposals.stream()
				.anyMatch(p -> p.getCompletionKind() == HQLCompletionProposal.FUNCTION
						&& p.getSimpleName().equals("substring")));
	}

	@Test
	public void testNoMetadata() {
		HQLCodeAssist noMetadataAssist = new HQLCodeAssist(null);
		List<String> errors = new ArrayList<>();
		noMetadataAssist.codeComplete("from ", 5, new IHQLCompletionRequestor() {
			@Override
			public boolean accept(HQLCompletionProposal proposal) {
				return true;
			}
			@Override
			public void completionFailure(String errorMessage) {
				errors.add(errorMessage);
			}
		});
		assertFalse(errors.isEmpty());
	}

	@Test
	public void testFindNearestWhiteSpace() {
		assertEquals(5, HQLCodeAssist.findNearestWhiteSpace("from Hello", 10));
		assertEquals(0, HQLCodeAssist.findNearestWhiteSpace("Hello", 5));
		assertEquals(6, HQLCodeAssist.findNearestWhiteSpace("from (Hello", 11));
	}

	@Test
	public void testEmptyQuery() {
		List<HQLCompletionProposal> proposals = complete("");
		assertFalse(proposals.isEmpty());
	}

	private List<HQLCompletionProposal> complete(String query) {
		List<HQLCompletionProposal> proposals = new ArrayList<>();
		codeAssist.codeComplete(query, query.length(), new IHQLCompletionRequestor() {
			@Override
			public boolean accept(HQLCompletionProposal proposal) {
				proposals.add(proposal);
				return true;
			}
			@Override
			public void completionFailure(String errorMessage) {
			}
		});
		return proposals;
	}

	private boolean hasProposalOfKind(List<HQLCompletionProposal> proposals, int kind) {
		return proposals.stream().anyMatch(p -> p.getCompletionKind() == kind);
	}
}
