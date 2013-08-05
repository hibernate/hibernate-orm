package org.hibernate.test.mapping;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "USER_CONFS")
@IdClass(UserConfId.class)
public class UserConfEntity implements Serializable{
	
	private static final long serialVersionUID = 9153314908821604322L;

	@Id
	@ManyToOne
	@JoinColumn(name="user_id", nullable = false)
	private UserEntity user;
	
	@Id
	@ManyToOne
	@JoinColumns({
			@JoinColumn(name="cnf_key", referencedColumnName="confKey"),
			@JoinColumn(name="cnf_value", referencedColumnName="confValue")})
	private ConfEntity conf;

	public ConfEntity getConf() {
		return conf;
	}

	public void setConf(ConfEntity conf) {
		this.conf = conf;
	}


	public UserEntity getUser() {
		return user;
	}

	public void setUser(UserEntity user) {
		this.user = user;
	}
}
