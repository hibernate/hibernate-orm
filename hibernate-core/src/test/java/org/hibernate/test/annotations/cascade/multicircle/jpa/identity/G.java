/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.cascade.multicircle.jpa.identity;

/**
 * No Documentation
 */
@javax.persistence.Entity
public class G extends AbstractEntity {
    private static final long serialVersionUID = 325417437L;

	@javax.persistence.ManyToOne(optional = false)
	private B b;

    @javax.persistence.OneToMany(mappedBy = "g")
    private java.util.Set<F> fCollection = new java.util.HashSet<F>();

	public B getB() {
		return b;
	}
	public void setB(B parameter){
		this.b = parameter;
	}

    public java.util.Set<F> getFCollection() {
        return fCollection;
    }
    public void setFCollection(
        java.util.Set<F> parameter) {
        this.fCollection = parameter;
    }
}
