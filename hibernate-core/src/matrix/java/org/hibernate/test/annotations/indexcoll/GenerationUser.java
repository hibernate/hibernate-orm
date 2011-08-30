//$Id$
package org.hibernate.test.annotations.indexcoll;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class GenerationUser {

	@Id
	@GeneratedValue
	private int id;

	@OneToMany
	@MapKey(name="generation")
	private Map<Generation, GenerationGroup> ref = new HashMap<Generation, GenerationGroup>();

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Map<Generation, GenerationGroup> getRef() {
		return ref;
	}

	public void setRef(Map<Generation, GenerationGroup> ref) {
		this.ref = ref;
	}


}
