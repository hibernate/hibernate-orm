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
public class E extends AbstractEntity {
    private static final long serialVersionUID = 1226955558L;

    /**
     * No documentation
     */
    @javax.persistence.ManyToOne(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
    , optional = false)
    private F f;

    public F getF() {
        return f;
    }

    public void setF(F parameter) {
        this.f = parameter;
    }
}
