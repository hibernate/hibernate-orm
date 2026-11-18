package org.hibernate.orm.test.annotations.tableperclass;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table( name = "xPM_Product",
		uniqueConstraints = {@UniqueConstraint( columnNames = {
		"manufacturerPartNumber", "manufacturerId"} )},
		indexes = @Index(name = "manufacturerPartNumber", columnList = "manufacturerPartNumber") )
public class Product extends Component {
}
