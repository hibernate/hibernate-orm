/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.ids;

import java.io.Serializable;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

/**
 * @author Slawek Garwol (slawekgarwol at gmail dot com)
 */
@Embeddable
@TypeDef(name = "customEnum", typeClass = CustomEnumUserType.class)
public class EmbIdWithCustomType implements Serializable {
	private Integer x;

	@Type(type = "customEnum")
	private CustomEnum customEnum;

	public EmbIdWithCustomType() {
	}

	public EmbIdWithCustomType(Integer x, CustomEnum customEnum) {
		this.x = x;
		this.customEnum = customEnum;
	}

	public Integer getX() {
		return x;
	}

	public void setX(Integer x) {
		this.x = x;
	}

	public CustomEnum getCustomEnum() {
		return customEnum;
	}

	public void setCustomEnum(CustomEnum customEnum) {
		this.customEnum = customEnum;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof EmbIdWithCustomType) ) {
			return false;
		}

		EmbIdWithCustomType that = (EmbIdWithCustomType) obj;

		if ( x != null ? !x.equals( that.x ) : that.x != null ) {
			return false;
		}
		if ( customEnum != that.customEnum ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result;
		result = (x != null ? x.hashCode() : 0);
		result = 31 * result + (customEnum != null ? customEnum.hashCode() : 0);
		return result;
	}
}
