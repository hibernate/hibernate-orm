//$Id$
package org.hibernate.test.annotations.generics;
import javax.persistence.MappedSuperclass;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class PricedStuff extends Stuff<Price> {
}
