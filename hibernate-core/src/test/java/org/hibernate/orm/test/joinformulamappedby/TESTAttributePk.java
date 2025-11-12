package org.hibernate.orm.test.joinformulamappedby;
import java.io.Serializable;
public class TESTAttributePk implements Serializable {

	private static final long serialVersionUID = -6497094163839794371L;
	
	private Class<?> entityClass;
	private long entityId;
	private String attrName;
	
	public TESTAttributePk()
	{
		
	}
	
	public TESTAttributePk(Class<?> entityClass, long entityId, String attrName)
	{
		this.setEntityClass(entityClass);
		this.setEntityId(entityId);
		this.setAttrName(attrName);
	}
	
	
	public Class<?> getEntityClass() {
		return entityClass;
	}
	public void setEntityClass(Class<?> entityClass) {
		this.entityClass = entityClass;
	}
	public long getEntityId() {
		return entityId;
	}
	public void setEntityId(long entityId) {
		this.entityId = entityId;
	}

	public String getAttrName() {
		return attrName;
	}

	public void setAttrName(String attrName) {
		this.attrName = attrName;
	}
	
	@Override
	public int hashCode() {
		return this.getAttrName().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (TESTAttributePk.class.isAssignableFrom(obj.getClass()))
			return false;
		TESTAttributePk a = (TESTAttributePk) obj;
		return a.getEntityClass().equals(this.getEntityClass()) && 
				a.getEntityId() == this.getEntityId() && 
				a.getAttrName().equals(this.getAttrName());
	}
}
