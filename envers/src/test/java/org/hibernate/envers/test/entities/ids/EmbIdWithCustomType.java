/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
        if (this == obj) return true;
        if (!(obj instanceof EmbIdWithCustomType)) return false;

        EmbIdWithCustomType that = (EmbIdWithCustomType) obj;

        if (x != null ? !x.equals(that.x) : that.x != null) return false;
        if (customEnum != that.customEnum) return false;

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
