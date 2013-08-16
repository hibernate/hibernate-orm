package org.hibernate.test.hql;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Panel implements Serializable {

    @Id
    private Long id;

    private Long clientId;

    private String deltaStamp;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getClientId() {
		return clientId;
	}

	public void setClientId(Long clientId) {
		this.clientId = clientId;
	}

	public String getDeltaStamp() {
		return deltaStamp;
	}

	public void setDeltaStamp(String deltaStamp) {
		this.deltaStamp = deltaStamp;
	}
}
