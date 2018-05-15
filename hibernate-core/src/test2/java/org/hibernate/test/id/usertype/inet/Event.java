/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.usertype.inet;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.TypeDef;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Event")
@Table(name = "event")
@TypeDef(name = "ipv4", typeClass = InetType.class, defaultForType = Inet.class)
public class Event {

	@Id
	private Long id;

	@Column(name = "ip", columnDefinition = "inet")
	private Inet ip;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Inet getIp() {
		return ip;
	}

	public void setIp(String address) {
		this.ip = new Inet( address );
	}
}
