/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cascade.circle.sequence;

/**
 * No Documentation
 */
@javax.persistence.Entity
public class G extends AbstractEntity {
    private static final long serialVersionUID = 325417437L;

    /**
     * No documentation
     */
    @javax.persistence.OneToMany(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
    , mappedBy = "g")
    private java.util.Set<org.hibernate.test.annotations.cascade.circle.sequence.C> cCollection = new java.util.HashSet<org.hibernate.test.annotations.cascade.circle.sequence.C>();

    public java.util.Set<org.hibernate.test.annotations.cascade.circle.sequence.C> getCCollection() {
        return cCollection;
    }

    public void setCCollection(
        java.util.Set<org.hibernate.test.annotations.cascade.circle.sequence.C> parameter) {
        this.cCollection = parameter;
    }
}
