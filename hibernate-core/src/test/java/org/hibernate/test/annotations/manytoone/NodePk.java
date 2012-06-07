//$Id$
package org.hibernate.test.annotations.manytoone;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class NodePk implements Serializable {
	private String name;
	private int level;

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof NodePk ) ) return false;

		final NodePk nodePk = (NodePk) o;

		if ( level != nodePk.level ) return false;
		if ( !name.equals( nodePk.name ) ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = name.hashCode();
		result = 29 * result + level;
		return result;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "fld_lvl")
	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}
}
