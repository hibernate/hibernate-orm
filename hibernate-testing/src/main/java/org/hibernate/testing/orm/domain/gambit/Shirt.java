/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.gambit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

/**
 * @author Chris Cranford
 */
@Entity
public class Shirt {
	@Id
	private Integer id;

	@Convert(converter = ShirtStringToIntegerConverter.class)
	private String data;

	@Enumerated
	@Column(name = "shirt_size")
	private Size size;

	@Enumerated(EnumType.STRING)
	private Color color;

	public enum Size {
		SMALL,
		MEDIUM,
		LARGE,
		XLARGE
	}

	public enum Color {
		WHITE,
		GREY,
		BLACK,
		BLUE,
		TAN
	}

	public static class ShirtStringToIntegerConverter implements AttributeConverter<String, Integer> {
		@Override
		public Integer convertToDatabaseColumn(String attribute) {
			if ( attribute != null ) {
				if ( attribute.equalsIgnoreCase( "X" ) ) {
					return 1;
				}
				else if ( attribute.equalsIgnoreCase( "Y" ) ) {
					return 2;
				}
			}
			return null;
		}

		@Override
		public String convertToEntityAttribute(Integer dbData) {
			if ( dbData != null ) {
				switch ( dbData ) {
					case 1:
						return "X";
					case 2:
						return "Y";
				}
			}
			return null;
		}
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Size getSize() {
		return size;
	}

	public void setSize(Size size) {
		this.size = size;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}
}
