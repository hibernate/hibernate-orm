//$Id$
package org.hibernate.test.annotations.entitynonentity;
import javax.persistence.MappedSuperclass;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Phone extends Voice {
	boolean isNumeric;
}
