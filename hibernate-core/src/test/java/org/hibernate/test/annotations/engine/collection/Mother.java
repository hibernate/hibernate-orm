package org.hibernate.test.annotations.engine.collection;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "co_mother")
public class Mother {
	@Id
	@GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) {  this.id = id; }
	private Integer id;

	@OneToMany(mappedBy = "mother")
	@Cascade({ CascadeType.SAVE_UPDATE })
	public Set<Son> getSons() { return sons; }
	public void setSons(Set<Son> sons) {  this.sons = sons; }
	private Set<Son> sons = new HashSet<Son>();
}
