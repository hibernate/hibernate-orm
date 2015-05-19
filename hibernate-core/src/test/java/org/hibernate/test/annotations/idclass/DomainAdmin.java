/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$

package org.hibernate.test.annotations.idclass;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedNativeQuery;
import javax.persistence.Table;


/**
 * A DomainAdmin.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
 */
@Entity
@Table(name = "domainadmin")
@IdClass(DomainAdminId.class)
@NamedNativeQuery(name = "DomainAdmin.testQuery",
		query = "select * from domainadmin da where da.domain_name = 'org'",
		resultClass = org.hibernate.test.annotations.idclass.DomainAdmin.class)
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
