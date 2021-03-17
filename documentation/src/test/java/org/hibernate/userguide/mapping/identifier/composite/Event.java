/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.identifier.composite;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
//tag::identifiers-composite-generated-mapping-example[]
@Entity
class Event {

    @Id
    private EventId id;

    @Column(name = "event_key")
    private String key;

    @Column(name = "event_value")
    private String value;

    //Getters and setters are omitted for brevity
//end::identifiers-composite-generated-mapping-example[]

    public EventId getId() {
        return id;
    }

    public void setId(EventId id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
//tag::identifiers-composite-generated-mapping-example[]
}
//end::identifiers-composite-generated-mapping-example[]

