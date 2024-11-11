/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import java.util.Set;


/**
 * @author Thomas Junk
 */
@JiraKey("HHH-18827")
@BytecodeEnhanced
@EnhancementOptions(biDirectionalAssociationManagement = true)
public class OneToManyAssociationWithSuperclassTest {

	@Test
	public void testEnhancementOneToManyMappedByProtected(){
		Person author = new Person();

		Book book = new Book();
		assertNull(book.getAuthor());

		author.setBooksWritten(Set.of(book));
		assertNotNull(book.getAuthor());
		assertEquals(author, book.getAuthor());
	}

	@Test
	@FailureExpected(
		jiraKey = "HHH-18827",
		reason = "Byte-code enhancement fails to find private target field in MappedSuperclass")
	public void testEnhancementOneToManyMappedByPrivate(){
		Person translator = new Person();

		Book book = new Book();
		assertNull(book.getTranslator());

		translator.setBooksTranslated(Set.of(book));
		assertNotNull(book.getTranslator());
		assertEquals(translator, book.getTranslator());
	}
}
