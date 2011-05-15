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
package org.hibernate.envers.test.integration.sortedSet;

import org.hibernate.envers.Audited;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Entity with custom-ordered SortedSet
 *
 * @author Michal Skowronek (mskowr at o2 pl)
 */
@Audited
public class NotAnnotatedSortedSetEntity {
    private Integer id;
    private String data;
    private SortedSet<NotAnnotatedStrTestEntity> sortedSet = new TreeSet<NotAnnotatedStrTestEntity>(
            NotAnnotatedStrTestEntityComparator.INSTANCE);

    public NotAnnotatedSortedSetEntity() {
    }

    public NotAnnotatedSortedSetEntity(Integer id, String data) {
        this.id = id;
        this.data = data;
    }

    public NotAnnotatedSortedSetEntity(String data) {
        this.data = data;
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

    public SortedSet<NotAnnotatedStrTestEntity> getSortedSet() {
        return sortedSet;
    }

    public void setSortedSet(SortedSet<NotAnnotatedStrTestEntity> sortedSet) {
        this.sortedSet = sortedSet;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotAnnotatedSortedSetEntity)) return false;

        NotAnnotatedSortedSetEntity that = (NotAnnotatedSortedSetEntity) o;

        if (data != null ? !data.equals(that.data) : that.data != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "SetOwnedEntity(id = " + id + ", data = " + data + ")";
    }
}
