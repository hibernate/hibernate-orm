//$Id$
package org.hibernate.test.annotations.lob;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Compiled code representation
 *
 * @author Emmanuel Bernard
 */
@Entity
public class CompiledCode extends AbstractCompiledCode {
	private Integer id;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

}
