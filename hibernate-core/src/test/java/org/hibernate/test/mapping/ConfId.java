package org.hibernate.test.mapping;

import java.io.Serializable;

public class ConfId  implements Serializable{

	private static final long serialVersionUID = -6722022851594514199L;

	private String confKey;

	private String confValue;
	
	public ConfId(){
	}

	public ConfId(String confKey, String confValue) {
		this.confKey = confKey;
		this.confValue = confValue;
	}

	public String getConfKey() {
		return confKey;
	}

	public void setConfKey(String confKey) {
		this.confKey = confKey;
	}

	public String getConfValue() {
		return confValue;
	}

	public void setConfValue(String confValue) {
		this.confValue = confValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((confKey == null) ? 0 : confKey.hashCode());
		result = prime * result + ((confValue == null) ? 0 : confValue.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConfId other = (ConfId) obj;
		if (confKey == null) {
			if (other.confKey != null)
				return false;
		} else if (!confKey.equals(other.confKey))
			return false;
		else if (confValue == null) {
			if (other.confValue != null)
				return false;
		} else if (!confValue.equals(other.confValue))
			return false;
		return true;
	}
}