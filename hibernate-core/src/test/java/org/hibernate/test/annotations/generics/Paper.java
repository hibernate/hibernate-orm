//$Id$
package org.hibernate.test.annotations.generics;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Paper extends Item<PaperType, SomeGuy> {
}
