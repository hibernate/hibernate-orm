/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.test.factory.puUtil;

import java.io.Serializable;

public class NestedLegacyEntityPk implements Serializable {

    private LegacyEntityPk legacyEntity;

    private int modernEntity;

    public NestedLegacyEntityPk() {
    }

    public LegacyEntityPk getLegacyEntity() {
        return legacyEntity;
    }

    public void setLegacyEntity(LegacyEntityPk legacyEntity) {
        this.legacyEntity = legacyEntity;
    }

    public int getModernEntity() {
        return modernEntity;
    }

    public void setModernEntity(int modernEntity) {
        this.modernEntity = modernEntity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NestedLegacyEntityPk that = (NestedLegacyEntityPk) o;

        if (modernEntity != that.modernEntity) return false;
        return legacyEntity != null ? legacyEntity.equals(that.legacyEntity) : that.legacyEntity == null;

    }

    @Override
    public int hashCode() {
        int result = legacyEntity != null ? legacyEntity.hashCode() : 0;
        result = 31 * result + modernEntity;
        return result;
    }
}
