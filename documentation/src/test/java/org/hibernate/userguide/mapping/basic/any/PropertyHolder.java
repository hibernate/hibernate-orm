/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic.any;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.Any;

//tag::mapping-column-any-example[]
@Entity
@Table( name = "property_holder" )
public class PropertyHolder {

    @Id
    private Long id;

    @Any(
        metaDef = "PropertyMetaDef",
        metaColumn = @Column( name = "property_type" )
    )
    @JoinColumn( name = "property_id" )
    private Property property;

	//Getters and setters are omitted for brevity

//end::mapping-column-any-example[]
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }
//tag::mapping-column-any-example[]
}
//end::mapping-column-any-example[]
