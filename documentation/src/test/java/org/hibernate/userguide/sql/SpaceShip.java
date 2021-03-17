/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.userguide.sql;

import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FetchType;
import javax.persistence.FieldResult;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;

/**
 * @author Emmanuel Bernard
 * @author Vlad MIhalcea
 */
//tag::sql-composite-key-entity-associations_named-query-example[]
@Entity
@NamedNativeQueries({
    @NamedNativeQuery(name = "find_all_spaceships",
        query =
            "SELECT " +
            "   name as \"name\", " +
            "   model, " +
            "   speed, " +
            "   lname as lastn, " +
            "   fname as firstn, " +
            "   length, " +
            "   width, " +
            "   length * width as surface, " +
            "   length * width * 10 as volume " +
            "FROM SpaceShip",
        resultSetMapping = "spaceship"
    )
})
@SqlResultSetMapping(
    name = "spaceship",
    entities = @EntityResult(
        entityClass = SpaceShip.class,
        fields = {
            @FieldResult(name = "name", column = "name"),
            @FieldResult(name = "model", column = "model"),
            @FieldResult(name = "speed", column = "speed"),
            @FieldResult(name = "captain.lastname", column = "lastn"),
            @FieldResult(name = "captain.firstname", column = "firstn"),
            @FieldResult(name = "dimensions.length", column = "length"),
            @FieldResult(name = "dimensions.width", column = "width"),
        }
    ),
    columns = {
        @ColumnResult(name = "surface"),
        @ColumnResult(name = "volume")
    }
)
public class SpaceShip {

    @Id
    private String name;

    private String model;

    private double speed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "fname", referencedColumnName = "firstname"),
        @JoinColumn(name = "lname", referencedColumnName = "lastname")
    })
    private Captain captain;

    private Dimensions dimensions;

    //Getters and setters are omitted for brevity

//end::sql-composite-key-entity-associations_named-query-example[]

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public Captain getCaptain() {
        return captain;
    }

    public void setCaptain(Captain captain) {
        this.captain = captain;
    }

    public Dimensions getDimensions() {
        return dimensions;
    }

    public void setDimensions(Dimensions dimensions) {
        this.dimensions = dimensions;
    }
//tag::sql-composite-key-entity-associations_named-query-example[]
}
//end::sql-composite-key-entity-associations_named-query-example[]
