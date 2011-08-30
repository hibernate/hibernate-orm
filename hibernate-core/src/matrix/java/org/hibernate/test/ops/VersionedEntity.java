package org.hibernate.test.ops;
import java.util.HashSet;
import java.util.Set;

/**
 * VersionedEntity
 *
 * @author Steve Ebersole
 */
public class VersionedEntity {
	private String id;
	private String name;
	private long version;

	private VersionedEntity parent;
	private Set children = new HashSet();

	public VersionedEntity() {
	}

	public VersionedEntity(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public VersionedEntity getParent() {
		return parent;
	}

	public void setParent(VersionedEntity parent) {
		this.parent = parent;
	}

	public Set getChildren() {
		return children;
	}

	public void setChildren(Set children) {
		this.children = children;
	}
}
