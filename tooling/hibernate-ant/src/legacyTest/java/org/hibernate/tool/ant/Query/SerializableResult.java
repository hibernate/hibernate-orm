package org.hibernate.tool.ant.Query;

import java.io.Serializable;

public class SerializableResult implements Serializable {

	public String id;
	public int length;

	@Override
	public String toString() {
		return "SerializableResult(id:" + id + ",length:" + length + ")";
	}

}
