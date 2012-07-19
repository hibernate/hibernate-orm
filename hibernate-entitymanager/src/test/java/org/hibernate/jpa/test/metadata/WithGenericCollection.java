/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.metadata;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
* This class has a List of mapped entity objects that are themselves parameterized.
* This class was added for JIRA issue #HHH-
*
* @author Kahli Burke
*/
@Entity
@Table(name = "WITH_GENERIC_COLLECTION")
public class WithGenericCollection<T> implements java.io.Serializable {
    @Id
    @Column(name = "ID")
    private String id;

    @Basic(optional=false)
    private double d;

    @ManyToOne(optional=false)
    @JoinColumn(name="PARENT_ID", insertable=false, updatable=false)
    private WithGenericCollection<? extends Object> parent = null;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="PARENT_ID")
    private List<WithGenericCollection<? extends Object>> children = new ArrayList<WithGenericCollection<? extends Object>>();

    public WithGenericCollection() {
    }

    //====================================================================
    // getters and setters for State fields

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setD(double d) {
        this.d = d;
    }

    public double getD() {
        return d;
    }

    public List<WithGenericCollection<? extends Object>> getChildren() {
        return children;
    }

    public void setChildren(List<WithGenericCollection<? extends Object>> children) {
        this.children = children;
    }


}
