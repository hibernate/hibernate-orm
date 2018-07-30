/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.userguide.sql;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 * @author Vlad MIhalcea
 */
//tag::sql-composite-key-entity-associations_named-query-example[]
@Embeddable
public class Dimensions {

    private int length;

    private int width;

    //Getters and setters are omitted for brevity

//end::sql-composite-key-entity-associations_named-query-example[]

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
//tag::sql-composite-key-entity-associations_named-query-example[]
}
//end::sql-composite-key-entity-associations_named-query-example[]
