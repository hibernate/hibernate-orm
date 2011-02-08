//$Id$
package org.hibernate.test.annotations.id.entities;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
@AttributeOverride(name = "longitude", column = @Column(name = "fld_longitude"))
public class Tower extends MilitaryBuilding {
}
