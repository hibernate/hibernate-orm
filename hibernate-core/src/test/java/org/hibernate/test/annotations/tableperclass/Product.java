//$Id$
package org.hibernate.test.annotations.tableperclass;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table( uniqueConstraints = {@UniqueConstraint( columnNames = {
		"manufacturerPartNumber", "manufacturerId"} )} )
public class Product extends Component {
}
