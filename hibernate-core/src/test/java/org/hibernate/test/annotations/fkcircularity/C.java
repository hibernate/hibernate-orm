// $Id$
package org.hibernate.test.annotations.fkcircularity;
import javax.persistence.Entity;

/**
 * Test entities ANN-722.
 * 
 * @author Hardy Ferentschik
 *
 */
@Entity
public class C extends B {
}
