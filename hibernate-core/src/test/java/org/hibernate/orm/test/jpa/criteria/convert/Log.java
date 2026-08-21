/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.convert;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Date;
@Entity
public class Log {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Convert(converter = DateConverter.class)
	private Date lastUpdate;
	@Converter
	static class DateConverter implements AttributeConverter<Date, Long> {
		@Override
		public Long convertToDatabaseColumn(Date date) {
			if (null == date) {
				return null;
			}
			return date.getTime();
		}
		@Override
		public Date convertToEntityAttribute(Long dbDate) {
			if (null == dbDate) {
				return null;
			}
			return new Date( dbDate );
		}
	}

	public Long getId() {
		return id;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}
}
