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
package org.hibernate.envers.test.integration.data;

import java.util.Arrays;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class LobTestEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Lob
    @Audited
    private String stringLob;

    @Lob
    @Audited
    private byte[] byteLob;

    @Lob
    @Audited
    private char[] charLob;

    public LobTestEntity() {
    }

    public LobTestEntity(String stringLob, byte[] byteLob, char[] charLob) {
        this.stringLob = stringLob;
        this.byteLob = byteLob;
        this.charLob = charLob;
    }

    public LobTestEntity(Integer id, String stringLob, byte[] byteLob, char[] charLob) {
        this.id = id;
        this.stringLob = stringLob;
        this.byteLob = byteLob;
        this.charLob = charLob;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStringLob() {
        return stringLob;
    }

    public void setStringLob(String stringLob) {
        this.stringLob = stringLob;
    }

    public byte[] getByteLob() {
        return byteLob;
    }

    public void setByteLob(byte[] byteLob) {
        this.byteLob = byteLob;
    }

    public char[] getCharLob() {
        return charLob;
    }

    public void setCharLob(char[] charLob) {
        this.charLob = charLob;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LobTestEntity)) return false;

        LobTestEntity that = (LobTestEntity) o;

        if (!Arrays.equals(byteLob, that.byteLob)) return false;
        if (!Arrays.equals(charLob, that.charLob)) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (stringLob != null ? !stringLob.equals(that.stringLob) : that.stringLob != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (stringLob != null ? stringLob.hashCode() : 0);
        result = 31 * result + (byteLob != null ? Arrays.hashCode(byteLob) : 0);
        result = 31 * result + (charLob != null ? Arrays.hashCode(charLob) : 0);
        return result;
    }
}