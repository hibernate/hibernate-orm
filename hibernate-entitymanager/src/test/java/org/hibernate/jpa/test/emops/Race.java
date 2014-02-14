//$Id$
package org.hibernate.jpa.test.emops;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Race {
	@Id @GeneratedValue
	public Integer id;
	
	@OrderColumn( name="index_" )
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@org.hibernate.annotations.Cascade( { org.hibernate.annotations.CascadeType.ALL }) 
	public List<Competitor> competitors = new ArrayList<Competitor>();
	
	public String name;
}
