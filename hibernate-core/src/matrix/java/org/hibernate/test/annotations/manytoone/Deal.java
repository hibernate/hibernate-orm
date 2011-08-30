//$Id$
package org.hibernate.test.annotations.manytoone;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Deal {
	@Id @GeneratedValue public Integer id;
	@ManyToOne @JoinColumn(referencedColumnName = "userId") public Customer from;
	@ManyToOne @JoinColumn(referencedColumnName = "userId") public Customer to;

}
