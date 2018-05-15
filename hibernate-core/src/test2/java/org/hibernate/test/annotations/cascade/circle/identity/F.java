/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cascade.circle.identity;

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
    private java.util.Set<B> bCollection = new java.util.HashSet<B>();

    /**
     * No documentation
     */
    @javax.persistence.OneToOne(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
    )
    private H h;

    public java.util.Set<B> getBCollection() {
        return bCollection;
    }

    public void setBCollection(
        java.util.Set<B> parameter) {
        this.bCollection = parameter;
    }

    public H getH() {
        return h;
    }

    public void setH(H parameter) {
        this.h = parameter;
    }
}
