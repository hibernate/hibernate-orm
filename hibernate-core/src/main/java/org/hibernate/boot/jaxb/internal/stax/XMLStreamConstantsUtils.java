/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal.stax;

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
		return switch ( eventId ) {
			case XMLStreamConstants.START_ELEMENT -> "StartElementEvent";
			case XMLStreamConstants.END_ELEMENT -> "EndElementEvent";
			case XMLStreamConstants.PROCESSING_INSTRUCTION -> "ProcessingInstructionEvent";
			case XMLStreamConstants.CHARACTERS -> "CharacterEvent";
			case XMLStreamConstants.COMMENT -> "CommentEvent";
			case XMLStreamConstants.START_DOCUMENT -> "StartDocumentEvent";
			case XMLStreamConstants.END_DOCUMENT -> "EndDocumentEvent";
			case XMLStreamConstants.ENTITY_REFERENCE -> "EntityReferenceEvent";
			case XMLStreamConstants.ATTRIBUTE -> "AttributeBase";
			case XMLStreamConstants.DTD -> "DTDEvent";
			case XMLStreamConstants.CDATA -> "CDATA";
			default -> "UNKNOWN_EVENT_TYPE";
		};
	}
}
