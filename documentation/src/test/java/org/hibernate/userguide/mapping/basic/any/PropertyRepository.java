/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.basic.any;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ManyToAny;

//tag::mapping-column-many-to-any-example[]
@Entity
@Table( name = "property_repository" )
public class PropertyRepository {

    @Id
    private Long id;

    @ManyToAny(
        metaDef = "PropertyMetaDef",
        metaColumn = @Column( name = "property_type" )
    )
    @Cascade( { org.hibernate.annotations.CascadeType.ALL })
    @JoinTable(name = "repository_properties",
        joinColumns = @JoinColumn(name = "repository_id"),
        inverseJoinColumns = @JoinColumn(name = "property_id")
    )
    private List<Property<?>> properties = new ArrayList<>(  );

	//Getters and setters are omitted for brevity

//end::mapping-column-many-to-any-example[]
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Property<?>> getProperties() {
        return properties;
    }

    //tag::mapping-column-many-to-any-example[]
}
//end::mapping-column-many-to-any-example[]
