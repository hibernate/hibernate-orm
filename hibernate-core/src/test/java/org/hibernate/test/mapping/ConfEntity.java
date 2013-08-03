package org.hibernate.test.mapping;

import static javax.persistence.CascadeType.ALL;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "CONF")
@IdClass(ConfId.class)
public class ConfEntity implements Serializable{

	private static final long serialVersionUID = -5089484717715507169L;

	@Id
	@Column(name = "confKey")
	private String confKey;

	@Id
	@Column(name = "confValue")
	private String confValue;

	@OneToMany(mappedBy="conf", cascade = ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private Set<UserConfEntity> userConf = new HashSet<UserConfEntity>();
	
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

	public Set<UserConfEntity> getUserConf() {
		return userConf;
	}
}
