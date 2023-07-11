package org.hibernate.orm.test.joinformulamappedby;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;

@Entity
@Table(name = "TEST_ATTR")
@FetchProfiles({
	@FetchProfile(name = "attrWithNode", fetchOverrides = {@FetchProfile.FetchOverride(entity=TESTAttribute.class, association="node", mode=FetchMode.JOIN)}),
	@FetchProfile(name = "attrWithLink", fetchOverrides = {@FetchProfile.FetchOverride(entity=TESTAttribute.class, association="link", mode=FetchMode.JOIN)})
})
@Inheritance(strategy = InheritanceType.JOINED)
public class TESTAttribute {

	@EmbeddedId
	@AttributeOverrides({
		@AttributeOverride(name="entityClass", column=@Column(name="ENTITY_CLASS", nullable = false, length = 255)),
		@AttributeOverride(name="entityId", column=@Column(name="ENTITY_ID", nullable = false)),
		@AttributeOverride(name="attrName", column=@Column(name="ATTR_NAME", length=255, nullable=false)),
	})
	private TESTAttributePk id;
	@Column(name="ENTITY_CLASS", insertable = false, updatable = false)
	private String entityClass;
	@Column(name="ENTITY_ID", insertable = false, updatable = false)
	private long entityId;
	@Column(name="ATTR_NAME", insertable = false, updatable = false)
	private String attrName;


	@JoinColumnOrFormula(
			column = @JoinColumn(name = "ENTITY_ID", referencedColumnName = "NODE_ID", nullable = false),
			formula = @JoinFormula("case when entity_class = 'com.cross_ni.cross.db.pojo.newcatest.TESTNode' then entity_id else null end"))
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	private TESTNode node;

	@JoinColumnOrFormula(
			column = @JoinColumn(name = "ENTITY_ID", referencedColumnName = "LINK_ID", nullable = false),
			formula = @JoinFormula("case when entity_class = 'com.cross_ni.cross.db.pojo.newcatest.TESTLink' then entity_id else null end"))
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	private TESTLink link;

	public TESTAttributePk getId() {
		return id;
	}

	public void setId(TESTAttributePk id) {
		this.id = id;
	}

	public String getAttrName() {
		return attrName;
	}

	public void setAttrName(String attrName) {
		this.attrName = attrName;
	}


	public String getEntityClass() {
		return entityClass;
	}

	public void setEntityClass(String entityClass) {
		this.entityClass = entityClass;
	}

	public long getEntityId() {
		return entityId;
	}

	public void setEntityId(long entityId) {
		this.entityId = entityId;
	}

	public TESTNode getNode() {
		return node;
	}

	public void setNode(TESTNode node) {
		this.node = node;
	}

	public TESTLink getLink() {
		return link;
	}

	public void setLink(TESTLink link) {
		this.link = link;
	}




}
