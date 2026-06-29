/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

class OverrideRepositoryDtdTest {

	@Test
	void dtdIsAvailableOnClasspath() {
		InputStream dtd = getClass().getClassLoader()
				.getResourceAsStream(
						"org/hibernate/hibernate-reverse-engineering-3.0.dtd");
		assertNotNull(dtd, "DTD should be loadable from the classpath");
	}

	@Test
	void entityResolverResolvesDtdLocally() throws Exception {
		InputSource source = OverrideRepository.DTD_RESOLVER.resolveEntity(
				null,
				"https://hibernate.org/dtd/hibernate-reverse-engineering-3.0.dtd");
		assertNotNull(source, "Entity resolver should resolve the DTD");
		assertNotNull(source.getByteStream(),
				"Resolved InputSource should have a stream");
	}

	@Test
	void parseRevengXmlWithoutNetworkAccess() {
		OverrideRepository repository = new OverrideRepository();
		assertDoesNotThrow(
				() -> repository.addResource(
						"org/hibernate/tool/reveng/internal/core/strategy/simple.reveng.xml"));
	}

	@Test
	void parseRevengXmlWithStrictJaxp() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setFeature(
				"http://apache.org/xml/features/nonvalidating/load-external-dtd",
				true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		db.setEntityResolver(OverrideRepository.DTD_RESOLVER);

		try (InputStream xml = getClass().getClassLoader()
				.getResourceAsStream(
						"org/hibernate/tool/reveng/internal/core/strategy/simple.reveng.xml")) {
			assertNotNull(xml);
			assertDoesNotThrow(() -> db.parse(xml));
		}
	}

	@Test
	void parseRevengXmlWithoutResolverFails() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setFeature(
				"http://apache.org/xml/features/nonvalidating/load-external-dtd",
				true);
		dbf.setFeature(
				"http://javax.xml.XMLConstants/feature/secure-processing",
				true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		db.setEntityResolver((publicId, systemId) -> {
			throw new java.io.IOException(
					"Simulated network restriction: " + systemId);
		});

		try (InputStream xml = getClass().getClassLoader()
				.getResourceAsStream(
						"org/hibernate/tool/reveng/internal/core/strategy/simple.reveng.xml")) {
			assertNotNull(xml);
			assertThrows(Exception.class, () -> db.parse(xml));
		}
	}
}
