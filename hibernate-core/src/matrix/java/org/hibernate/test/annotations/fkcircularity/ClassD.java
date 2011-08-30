// $Id$
package org.hibernate.test.annotations.fkcircularity;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * Test entities ANN-730.
 * 
 * @author Hardy Ferentschik
 *
 */
@Entity
@Table(name = "class_1d")
@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
public class ClassD extends ClassC {
}
