/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal.stax;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;

import org.hibernate.boot.xsd.ConfigXsdSupport;

/**
 * @author Steve Ebersole
 */
public class ConfigurationEventReader extends AbstractEventReader {
	private static final String ROOT_ELEMENT_NAME = "persistence";

	public ConfigurationEventReader(XMLEventReader reader, XMLEventFactory xmlEventFactory) {
		super(
				ROOT_ELEMENT_NAME,
				ConfigXsdSupport.configurationXsd(),
				reader,
				xmlEventFactory
		);
	}

	@Override
	protected boolean shouldBeMappedToLatestJpaDescriptor(String uri) {
		return !ConfigXsdSupport.configurationXsd().getNamespaceUri().equals( uri );
	}
}
