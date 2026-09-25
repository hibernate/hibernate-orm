package org.hibernate.orm.test.mapping.collections.defaultsemantics.modulefixtures.list;

import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/// @author Steve Ebersole
@Entity
public class ListOwner {
	@Id
	public Integer id;
	@ElementCollection
	public List<String> names;
}
