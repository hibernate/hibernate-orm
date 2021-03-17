/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.userguide.sql;
import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 * @author Vlad MIhalcea
 */
//tag::sql-composite-key-entity-associations_named-query-example[]
@Embeddable
public class Identity implements Serializable {

    private String firstname;

    private String lastname;

    //Getters and setters are omitted for brevity

//end::sql-composite-key-entity-associations_named-query-example[]

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

//tag::sql-composite-key-entity-associations_named-query-example[]
    public boolean equals(Object o) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        final Identity identity = (Identity) o;

        if ( !firstname.equals( identity.firstname ) ) return false;
        if ( !lastname.equals( identity.lastname ) ) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = firstname.hashCode();
        result = 29 * result + lastname.hashCode();
        return result;
    }
}
//end::sql-composite-key-entity-associations_named-query-example[]
