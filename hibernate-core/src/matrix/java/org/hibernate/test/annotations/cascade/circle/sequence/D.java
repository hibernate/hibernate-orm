/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.test.annotations.cascade.circle.sequence;

/**
 * No Documentation
 */
@javax.persistence.Entity
public class D extends AbstractEntity {
    private static final long serialVersionUID = 2417176961L;

    /**
     * No documentation
     */
    @javax.persistence.ManyToMany(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
    )
    private java.util.Set<org.hibernate.test.annotations.cascade.circle.sequence.A> aCollection = new java.util.HashSet<org.hibernate.test.annotations.cascade.circle.sequence.A>();

    /**
     * No documentation
     */
    @javax.persistence.OneToMany(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
    )
    private java.util.Set<E> eCollection = new java.util.HashSet<E>();

    public java.util.Set<org.hibernate.test.annotations.cascade.circle.sequence.A> getACollection() {
        return aCollection;
    }

    public void setACollection(
        java.util.Set<org.hibernate.test.annotations.cascade.circle.sequence.A> parameter) {
        this.aCollection = parameter;
    }

    public java.util.Set<E> getECollection() {
        return eCollection;
    }

    public void setECollection(
        java.util.Set<E> parameter) {
        this.eCollection = parameter;
    }
}
