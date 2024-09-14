/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.idclass;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.Table;


/**
 * A DomainAdmin.
 *
 * @author Stale W. Pedersen
 */
@Entity
@Table(name = "domainadmin")
@IdClass(DomainAdminId.class)
@NamedNativeQuery(name = "DomainAdmin.testQuery",
		query = "select * from domainadmin da where da.domain_name = 'org'",
		resultClass = DomainAdmin.class)
public class DomainAdmin implements Serializable {

	@Id
	@Column(name = "domain_name")
	private String domainName;

	@Id
	@Column(name = "admin_user")
	private String adminUser;

	@Column(name = "nick_name")
	private String nickName;

	public DomainAdmin() {
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public String getAdminUser() {
		return adminUser;
	}

	public void setAdminUser(String adminUser) {
		this.adminUser = adminUser;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}
}
