/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.xml.internal.stax;

import javax.xml.stream.XMLStreamConstants;

/**
 * Note, copied from the uPortal project by permission of author.  See
 * https://github.com/Jasig/uPortal/blob/master/uportal-war/src/main/java/org/jasig/portal/xml/stream/XMLStreamConstantsUtils.java
 *
 * @author Eric Dalquist
 */
public final class XMLStreamConstantsUtils {
	private XMLStreamConstantsUtils() {
	}

	/**
	 * Get the human readable event name for the numeric event id
	 */
	public static String getEventName(int eventId) {
		switch (eventId) {
			case XMLStreamConstants.START_ELEMENT:
				return "StartElementEvent";
			case XMLStreamConstants.END_ELEMENT:
				return "EndElementEvent";
			case XMLStreamConstants.PROCESSING_INSTRUCTION:
				return "ProcessingInstructionEvent";
			case XMLStreamConstants.CHARACTERS:
				return "CharacterEvent";
			case XMLStreamConstants.COMMENT:
				return "CommentEvent";
			case XMLStreamConstants.START_DOCUMENT:
				return "StartDocumentEvent";
			case XMLStreamConstants.END_DOCUMENT:
				return "EndDocumentEvent";
			case XMLStreamConstants.ENTITY_REFERENCE:
				return "EntityReferenceEvent";
			case XMLStreamConstants.ATTRIBUTE:
				return "AttributeBase";
			case XMLStreamConstants.DTD:
				return "DTDEvent";
			case XMLStreamConstants.CDATA:
				return "CDATA";
		}
		return "UNKNOWN_EVENT_TYPE";
	}
}
