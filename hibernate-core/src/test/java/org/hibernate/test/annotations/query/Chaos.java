//$Id$
package org.hibernate.test.annotations.query;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedNativeQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name="CHAOS")
@SQLInsert( sql="INSERT INTO CHAOS(name, nick_name, chaos_size, id) VALUES(upper(?),?,?,?)")
@SQLUpdate( sql="UPDATE CHAOS SET name = upper(?), nick_name = ?, chaos_size = ? WHERE id = ?")
@SQLDelete( sql="DELETE CHAOS WHERE id = ?")
@SQLDeleteAll( sql="DELETE CHAOS")
@Loader(namedQuery = "chaos")
@NamedNativeQuery(name="chaos", query="select id, chaos_size, name, lower( nick_name ) as nick_name from CHAOS where id= ?", resultClass = Chaos.class)
public class Chaos {
	@Id
	private Long id;
	@Column(name="chaos_size")
	private Long size;
	private String name;
	@Column(name="nick_name")
	private String nickname;

	@OneToMany
	@JoinColumn(name="chaos_fk")
	@SQLInsert( sql="UPDATE CASIMIR_PARTICULE SET chaos_fk = ? where id = ?")
	@SQLDelete( sql="UPDATE CASIMIR_PARTICULE SET chaos_fk = null where id = ?")
	private Set<CasimirParticle> particles = new HashSet<CasimirParticle>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public Set<CasimirParticle> getParticles() {
		return particles;
	}

	public void setParticles(Set<CasimirParticle> particles) {
		this.particles = particles;
	}
}
