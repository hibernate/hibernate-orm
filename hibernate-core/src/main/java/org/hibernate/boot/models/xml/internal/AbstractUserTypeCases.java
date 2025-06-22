/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal;

import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeImpl;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.BigDecimalJavaType;
import org.hibernate.type.descriptor.java.BigIntegerJavaType;
import org.hibernate.type.descriptor.java.BlobJavaType;
import org.hibernate.type.descriptor.java.BooleanJavaType;
import org.hibernate.type.descriptor.java.ByteJavaType;
import org.hibernate.type.descriptor.java.CalendarJavaType;
import org.hibernate.type.descriptor.java.CharacterJavaType;
import org.hibernate.type.descriptor.java.ClassJavaType;
import org.hibernate.type.descriptor.java.ClobJavaType;
import org.hibernate.type.descriptor.java.CurrencyJavaType;
import org.hibernate.type.descriptor.java.DateJavaType;
import org.hibernate.type.descriptor.java.DoubleJavaType;
import org.hibernate.type.descriptor.java.DurationJavaType;
import org.hibernate.type.descriptor.java.FloatJavaType;
import org.hibernate.type.descriptor.java.InetAddressJavaType;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.IntegerJavaType;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
import org.hibernate.type.descriptor.java.LocaleJavaType;
import org.hibernate.type.descriptor.java.LongJavaType;
import org.hibernate.type.descriptor.java.NClobJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.java.ShortJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.java.TimeZoneJavaType;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.java.UrlJavaType;
import org.hibernate.type.descriptor.java.YearJavaType;
import org.hibernate.type.descriptor.java.ZoneIdJavaType;
import org.hibernate.type.descriptor.java.ZoneOffsetJavaType;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaType;

import jakarta.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractUserTypeCases implements UserTypeCases {
	protected abstract void applyJavaTypeAnnotation(
			MutableMemberDetails memberDetails,
			Class<? extends BasicJavaType<?>> descriptor,
			XmlDocumentContext xmlDocumentContext);

	protected abstract void applyTemporalPrecision(
			MutableMemberDetails memberDetails,
			@SuppressWarnings("deprecation") TemporalType temporalType,
			XmlDocumentContext xmlDocumentContext);

	@Override
	public void handleNone(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
	}

	@Override
	public void handleCharacter(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, CharacterJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleString(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, StringJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleByte(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, ByteJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleBoolean(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, BooleanJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleShort(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, ShortJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleInteger(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, IntegerJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleLong(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, LongJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleDouble(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, DoubleJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleFloat(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, FloatJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleBigInteger(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, BigIntegerJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleBigDecimal(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, BigDecimalJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleUuid(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, UUIDJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleUrl(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, UrlJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleInetAddress(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, InetAddressJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleCurrency(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, CurrencyJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleLocale(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, LocaleJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleClass(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, ClassJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleBlob(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, BlobJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleClob(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, ClobJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleNClob(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, NClobJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleInstant(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, InstantJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleDuration(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, DurationJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleYear(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, YearJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleLocalDateTime(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, LocalDateTimeJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleLocalDate(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, LocalDateJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleLocalTime(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, LocalTimeJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleZonedDateTime(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, ZonedDateTimeJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleOffsetDateTime(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, OffsetDateTimeJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleOffsetTime(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, OffsetTimeJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleZoneId(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, ZoneIdJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleZoneOffset(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, ZoneOffsetJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleTimestamp(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, DateJavaType.class, xmlDocumentContext );
		//noinspection deprecation
		applyTemporalPrecision( memberDetails, TemporalType.TIMESTAMP, xmlDocumentContext );
	}

	@Override
	public void handleDate(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, DateJavaType.class, xmlDocumentContext );
		//noinspection deprecation
		applyTemporalPrecision( memberDetails, TemporalType.DATE, xmlDocumentContext );
	}

	@Override
	public void handleTime(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, DateJavaType.class, xmlDocumentContext );
		//noinspection deprecation
		applyTemporalPrecision( memberDetails, TemporalType.TIME, xmlDocumentContext );
	}

	@Override
	public void handleCalendar(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, CalendarJavaType.class, xmlDocumentContext );
	}

	@Override
	public void handleTimeZone(
			JaxbUserTypeImpl jaxbType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		applyJavaTypeAnnotation( memberDetails, TimeZoneJavaType.class, xmlDocumentContext );
	}
}
