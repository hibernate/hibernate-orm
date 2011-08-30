//$Id$
package org.hibernate.test.annotations.manytoone;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * TODO: change this sample with an Address -> Country relation. This is more accurate
 *
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "tbl_child")
public class Child implements Serializable {
	@Id
	@GeneratedValue
	public Integer id;

	@ManyToOne()
	@JoinColumns({
	@JoinColumn(name = "parentCivility", referencedColumnName = "isMale"),
	@JoinColumn(name = "parentLastName", referencedColumnName = "lastName"),
	@JoinColumn(name = "parentFirstName", referencedColumnName = "firstName")
			})
	public Parent parent;
}
