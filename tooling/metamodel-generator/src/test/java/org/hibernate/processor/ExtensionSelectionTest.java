package org.hibernate.processor;

import java.util.ArrayList;

import org.hibernate.processor.internal.DefaultHibernateProcessorExtension;
import org.junit.jupiter.api.Test;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionSelectionTest {

	@Test
	void defaultExtensionWhenNoneRegistered() {
		final var errors = new ArrayList<String>();
		final var extension = HibernateProcessor.selectExtension( of(), errors::add );
		assertInstanceOf( DefaultHibernateProcessorExtension.class, extension );
		assertTrue( errors.isEmpty() );
	}

	@Test
	void singleRegisteredExtensionIsUsed() {
		final var errors = new ArrayList<String>();
		final var only = new FirstExtension();
		assertSame( only, HibernateProcessor.selectExtension( of( only ), errors::add ) );
		assertTrue( errors.isEmpty() );
	}

	@Test
	void severalRegisteredExtensionsAreAnError() {
		final var errors = new ArrayList<String>();
		HibernateProcessor.selectExtension( of( new FirstExtension(), new SecondExtension() ), errors::add );
		assertEquals( 1, errors.size() );
		assertTrue( errors.get( 0 ).contains( FirstExtension.class.getName() ), errors.get( 0 ) );
		assertTrue( errors.get( 0 ).contains( SecondExtension.class.getName() ), errors.get( 0 ) );
	}

	static class FirstExtension extends DefaultHibernateProcessorExtension {
	}

	static class SecondExtension extends DefaultHibernateProcessorExtension {
	}
}
