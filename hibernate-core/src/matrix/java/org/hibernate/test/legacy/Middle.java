package org.hibernate.test.legacy;
import java.io.Serializable;

/**
 * @author Stefano Travelli
 */
public class Middle implements Serializable {
	private MiddleKey id;
	private String bla;

	public MiddleKey getId() {
		return id;
	}

	public void setId(MiddleKey id) {
		this.id = id;
	}

	public String getBla() {
		return bla;
	}

	public void setBla(String bla) {
		this.bla = bla;
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Middle)) return false;

		final Middle cidMaster = (Middle) o;

		if (id != null ? !id.equals(cidMaster.id) : cidMaster.id != null) return false;

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}
}
