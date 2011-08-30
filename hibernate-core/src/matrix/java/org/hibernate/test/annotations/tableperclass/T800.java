//$Id$
package org.hibernate.test.annotations.tableperclass;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class T800 extends Robot {
	public String getTargetName() {
		return targetName;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	private String targetName;
}
