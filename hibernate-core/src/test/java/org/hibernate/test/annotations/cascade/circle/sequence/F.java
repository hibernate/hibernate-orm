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
public class F extends AbstractEntity {
    private static final long serialVersionUID = 1471534025L;

    /**
     * No documentation
     */
    @javax.persistence.OneToMany(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
    , mappedBy = "f")
    private java.util.Set<org.hibernate.test.annotations.cascade.circle.sequence.B> bCollection = new java.util.HashSet<org.hibernate.test.annotations.cascade.circle.sequence.B>();

    /**
     * No documentation
     */
    @javax.persistence.OneToOne(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
    )
    private H h;

    public java.util.Set<org.hibernate.test.annotations.cascade.circle.sequence.B> getBCollection() {
        return bCollection;
    }

    public void setBCollection(
        java.util.Set<org.hibernate.test.annotations.cascade.circle.sequence.B> parameter) {
        this.bCollection = parameter;
    }

    public H getH() {
        return h;
    }

    public void setH(H parameter) {
        this.h = parameter;
    }
}
