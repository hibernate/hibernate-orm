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
package org.hibernate.envers.test.entities.manytomany.biowned;

import java.util.List;
import java.util.ArrayList;
import javax.persistence.*;

import org.hibernate.envers.Audited;

/**
 * Entity owning a many-to-many relation, where the other entity also owns the relation.
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class ListBiowning1Entity {
    @Id    
    @GeneratedValue
    private Integer id;

    private String data;

    @ManyToMany
    @JoinTable(
		name = "biowning",
		joinColumns = @JoinColumn(name = "biowning1_id"),
		inverseJoinColumns = @JoinColumn(name = "biowning2_id", insertable = false, updatable = false)
	)
    private List<ListBiowning2Entity> references = new ArrayList<ListBiowning2Entity>();

    public ListBiowning1Entity() { }

    public ListBiowning1Entity(Integer id, String data) {
        this.id = id;
        this.data = data;
    }

    public ListBiowning1Entity(String data) {
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

    public List<ListBiowning2Entity> getReferences() {
        return references;
    }

    public void setReferences(List<ListBiowning2Entity> references) {
        this.references = references;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListBiowning1Entity)) return false;

        ListBiowning1Entity that = (ListBiowning1Entity) o;

        if (data != null ? !data.equals(that.data) : that.data != null) return false;
        //noinspection RedundantIfStatement
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
        return "ListBiowning1Entity(id = " + id + ", data = " + data + ")";
    }
}