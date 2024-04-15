/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

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
				switch ( Integer.valueOf( dbData ) ) {
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
