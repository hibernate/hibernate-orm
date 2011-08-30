//$Id$
package org.hibernate.test.annotations.quote;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "`User`")
public class User implements Serializable {

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long id;

   @ManyToMany
   private Set<Role> roles = new HashSet<Role>();


	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Set<Role> getRoles() {
		return roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}
}
