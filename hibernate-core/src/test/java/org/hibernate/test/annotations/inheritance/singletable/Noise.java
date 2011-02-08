//$Id$
package org.hibernate.test.annotations.inheritance.singletable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
@DiscriminatorValue("0")
public class Noise extends Music {
}
