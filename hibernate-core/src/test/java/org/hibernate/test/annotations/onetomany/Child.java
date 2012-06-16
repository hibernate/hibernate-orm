//$Id$
package org.hibernate.test.annotations.onetomany;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Child implements Serializable {
	@Id
	@GeneratedValue
	public Integer id;

	@ManyToOne()
	@JoinColumns({
	@JoinColumn(name = "parentCivility", referencedColumnName = "isMale"),
	@JoinColumn(name = "parentLastName", referencedColumnName = "lastName"),
	@JoinColumn(name = "parentFirstName", referencedColumnName = "firstName")
			})
	public Parent parent;
	@Column(name = "fav_sup_hero")
	public String favoriteSuperhero;
	@Column(name = "fav_singer")
	public String favoriteSinger;
}
