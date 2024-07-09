package org.hibernate.orm.test.joinformulamappedby;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.FetchType;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import java.util.Set;
import org.hibernate.annotations.Where;


@Entity
@Table(name="TEST_NODE")
public class TESTNode {

	@Id
	@Column(name = "NODE_ID", nullable = false)
	private long id;

	@Column(name = "NAME", nullable = false, length = 255)
	private String name;
        @Where(clause = "entity_class = 'org.hibernate.orm.test.joinformulamappedby.TESTNode'")
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "node")
	private Set<TESTAttribute> attributes;
	
	protected TESTNode()
	{

	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
