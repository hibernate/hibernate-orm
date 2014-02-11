package org.hibernate.test.collection.set;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class PersonInfoEntity implements Serializable {

	private static final long serialVersionUID = 2252230466609330843L;

	private long id;

	private Set<PersonInfo> infos = new HashSet<PersonInfo>();

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Set<PersonInfo> getInfos() {
		return infos;
	}

	public void setInfos(Set<PersonInfo> infos) {
		this.infos = infos;
	}

}
