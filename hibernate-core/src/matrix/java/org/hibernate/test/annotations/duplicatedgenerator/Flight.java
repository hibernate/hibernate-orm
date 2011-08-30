//$Id$
package org.hibernate.test.annotations.duplicatedgenerator;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Here to test duplicate import
 *
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "tbl_flight")
public class Flight {
	@Id
	public String id;
}
