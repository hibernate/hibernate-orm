/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
				ConfigXsdSupport.getJPA32(),
				reader,
				xmlEventFactory
		);
	}
}
