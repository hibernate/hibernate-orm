//$Id$
package org.hibernate.test.annotations.inheritance.singletable;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
@DiscriminatorColumn(discriminatorType = DiscriminatorType.INTEGER)
public class Trash {
	@Id
	@GeneratedValue
	private Integer id;
}
