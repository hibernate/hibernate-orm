//$Id$
package org.hibernate.test.annotations.indexcoll;
import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class PaintingPk implements Serializable {
	private String name;
	private String painter;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPainter() {
		return painter;
	}

	public void setPainter(String painter) {
		this.painter = painter;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		final PaintingPk that = (PaintingPk) o;

		if ( !name.equals( that.name ) ) return false;
		if ( !painter.equals( that.painter ) ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = name.hashCode();
		result = 29 * result + painter.hashCode();
		return result;
	}
}
