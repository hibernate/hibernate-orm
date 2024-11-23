/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetoone.cache;
import java.io.Serializable;

public abstract class Person implements Serializable {
    private int id;
    private String name;
    private Details details;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public Details getDetails() {
        return details;
    }

    public void setDetails(Details details) {
        if (details != null) {
            details.setPerson(this);
        }

        this.details = details;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
