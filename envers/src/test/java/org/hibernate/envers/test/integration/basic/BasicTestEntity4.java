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
package org.hibernate.envers.test.integration.basic;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class BasicTestEntity4 {
    @Id
    @GeneratedValue
    private Integer id;

    private String str1;

    private String str2;

    public BasicTestEntity4() {
    }

    public BasicTestEntity4(String str1, String str2) {
        this.str1 = str1;
        this.str2 = str2;
    }

    public BasicTestEntity4(Integer id, String str1, String str2) {
        this.id = id;
        this.str1 = str1;
        this.str2 = str2;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStr1() {
        return str1;
    }

    public void setStr1(String str1) {
        this.str1 = str1;
    }

    public String getStr2() {
        return str2;
    }

    public void setStr2(String str2) {
        this.str2 = str2;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BasicTestEntity4)) return false;

        BasicTestEntity4 that = (BasicTestEntity4) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (str1 != null ? !str1.equals(that.str1) : that.str1 != null) return false;
        if (str2 != null ? !str2.equals(that.str2) : that.str2 != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (str1 != null ? str1.hashCode() : 0);
        result = 31 * result + (str2 != null ? str2.hashCode() : 0);
        return result;
    }
}