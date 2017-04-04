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
public class B extends AbstractEntity {
    private static final long serialVersionUID = 325417243L;

    /**
     * No documentation
     */
    @javax.persistence.OneToMany(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
    , mappedBy = "b")
    private java.util.Set<C> cCollection = new java.util.HashSet<C>();

    /**
     * No documentation
     */
    @javax.persistence.ManyToOne(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
    , optional = false)
    private A a;

    /**
     * No documentation
     */
    @javax.persistence.ManyToOne(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
    , optional = false)
    private F f;

    public java.util.Set<C> getCCollection() {
        return cCollection;
    }

    public void setCCollection(
        java.util.Set<C> parameter) {
        this.cCollection = parameter;
    }

    public A getA() {
        return a;
    }

    public void setA(A parameter) {
        this.a = parameter;
    }

    public F getF() {
        return f;
    }

    public void setF(F parameter) {
        this.f = parameter;
    }
}
