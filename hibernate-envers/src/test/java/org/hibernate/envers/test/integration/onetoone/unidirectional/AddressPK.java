package org.hibernate.envers.test.integration.onetoone.unidirectional;

import java.io.Serializable;

/**
 * @author Andrei Zagorneanu
 */
public class AddressPK implements Serializable {
	private Integer id;
	private Integer ver;

	public AddressPK() {
	}

	public AddressPK(Integer id, Integer ver) {
		this.setId(id);
		this.setVer(ver);
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getVer() {
		return ver;
	}

	public void setVer(Integer ver) {
		this.ver = ver;
	}

	public String toString() {
		return "AddressPK[id = " + getId() + ", ver = " + getVer() + "]";
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + ((id == null) ? 0 : id.hashCode());
		result = 31 * result + ((ver == null) ? 0 : ver.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AddressPK)) return false;

        AddressPK that = (AddressPK) o;

        if (ver != null ? !ver.equals(that.ver) : that.ver != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
	}
}
