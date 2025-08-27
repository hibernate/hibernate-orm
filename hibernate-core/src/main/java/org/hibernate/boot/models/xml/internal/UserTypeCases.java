/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal;

import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeImpl;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.MutableMemberDetails;

/**
 * @author Steve Ebersole
 */
public interface UserTypeCases {
	void handleNone(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleCharacter(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleString(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleByte(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleBoolean(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleShort(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleInteger(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleLong(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleDouble(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleFloat(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleBigInteger(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleBigDecimal(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleUuid(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleUrl(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleInetAddress(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleCurrency(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleLocale(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleClass(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleBlob(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleClob(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleNClob(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleInstant(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleDuration(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleYear(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleLocalDateTime(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleLocalDate(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleLocalTime(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleZonedDateTime(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleOffsetDateTime(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleOffsetTime(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleZoneId(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleZoneOffset(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleTimestamp(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleDate(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleTime(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleCalendar(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleTimeZone(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);

	void handleGeneral(JaxbUserTypeImpl jaxbType, MutableMemberDetails memberDetails, XmlDocumentContext xmlDocumentContext);
}
