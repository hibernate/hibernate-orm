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
package org.hibernate.envers.test.integration.secondary.ids;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.SecondaryTable;

import org.hibernate.envers.SecondaryAuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.ids.MulId;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@SecondaryTable(name = "secondary")
@SecondaryAuditTable(secondaryTableName = "secondary", secondaryAuditTableName = "sec_mulid_versions")
@Audited
@IdClass(MulId.class)
public class SecondaryMulIdTestEntity {
    @Id
    private Integer id1;

    @Id
    private Integer id2;

    private String s1;

    @Column(table = "secondary")
    private String s2;

    public SecondaryMulIdTestEntity(MulId id, String s1, String s2) {
        this.id1 = id.getId1();
        this.id2 = id.getId2();
        this.s1 = s1;
        this.s2 = s2;
    }

    public SecondaryMulIdTestEntity(String s1, String s2) {
        this.s1 = s1;
        this.s2 = s2;
    }

    public SecondaryMulIdTestEntity() {
    }

    public Integer getId1() {
        return id1;
    }

    public void setId1(Integer id1) {
        this.id1 = id1;
    }

    public Integer getId2() {
        return id2;
    }

    public void setId2(Integer id2) {
        this.id2 = id2;
    }

    public String getS1() {
        return s1;
    }

    public void setS1(String s1) {
        this.s1 = s1;
    }

    public String getS2() {
        return s2;
    }

    public void setS2(String s2) {
        this.s2 = s2;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecondaryMulIdTestEntity)) return false;

        SecondaryMulIdTestEntity that = (SecondaryMulIdTestEntity) o;

        if (id1 != null ? !id1.equals(that.id1) : that.id1 != null) return false;
        if (id2 != null ? !id2.equals(that.id2) : that.id2 != null) return false;
        if (s1 != null ? !s1.equals(that.s1) : that.s1 != null) return false;
        if (s2 != null ? !s2.equals(that.s2) : that.s2 != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id1 != null ? id1.hashCode() : 0);
        result = 31 * result + (id2 != null ? id2.hashCode() : 0);
        result = 31 * result + (s1 != null ? s1.hashCode() : 0);
        result = 31 * result + (s2 != null ? s2.hashCode() : 0);
        return result;
    }
}