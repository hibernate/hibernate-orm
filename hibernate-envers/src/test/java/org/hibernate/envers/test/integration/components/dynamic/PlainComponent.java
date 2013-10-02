package org.hibernate.envers.test.integration.components.dynamic;

import java.util.ArrayList;
import java.util.List;

public class PlainComponent {

	private String componentNote;
	private List<ManyToManyEntity> manyToManyList = new ArrayList<ManyToManyEntity>();
	private OneToOneEntity oneToOneEntity;
	private ManyToOneEntity manyToOneEntity;
	private InternalComponent internalComponent;
	private List<InternalComponent> internalComponents;

	public String getComponentNote() {
		return componentNote;
	}

	public void setComponentNote(String componentNote) {
		this.componentNote = componentNote;
	}

	public List<ManyToManyEntity> getManyToManyList() {
		return manyToManyList;
	}

	public void setManyToManyList(List<ManyToManyEntity> manyToManyList) {
		this.manyToManyList = manyToManyList;
	}

	public OneToOneEntity getOneToOneEntity() {
		return oneToOneEntity;
	}

	public void setOneToOneEntity(OneToOneEntity oneToOneEntity) {
		this.oneToOneEntity = oneToOneEntity;
	}

	public ManyToOneEntity getManyToOneEntity() {
		return manyToOneEntity;
	}

	public void setManyToOneEntity(ManyToOneEntity manyToOneEntity) {
		this.manyToOneEntity = manyToOneEntity;
	}

	public InternalComponent getInternalComponent() {
		return internalComponent;
	}

	public void setInternalComponent(InternalComponent internalComponent) {
		this.internalComponent = internalComponent;
	}

	public List<InternalComponent> getInternalComponents() {
		return internalComponents;
	}

	public void setInternalComponents(List<InternalComponent> internalComponents) {
		this.internalComponents = internalComponents;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof PlainComponent ) ) {
			return false;
		}

		PlainComponent that = (PlainComponent) o;

		if ( componentNote != null ? !componentNote.equals( that.componentNote ) : that.componentNote != null ) {
			return false;
		}
		if ( internalComponent != null ? !internalComponent.equals( that.internalComponent ) : that.internalComponent != null ) {
			return false;
		}
		if ( internalComponents != null ? !internalComponents.equals( that.internalComponents ) : that.internalComponents != null ) {
			return false;
		}
		if ( manyToManyList != null ? !manyToManyList.equals( that.manyToManyList ) : that.manyToManyList != null ) {
			return false;
		}
		if ( manyToOneEntity != null ? !manyToOneEntity.equals( that.manyToOneEntity ) : that.manyToOneEntity != null ) {
			return false;
		}
		if ( oneToOneEntity != null ? !oneToOneEntity.equals( that.oneToOneEntity ) : that.oneToOneEntity != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = componentNote != null ? componentNote.hashCode() : 0;
		result = 31 * result + ( manyToManyList != null ? manyToManyList.hashCode() : 0 );
		result = 31 * result + ( oneToOneEntity != null ? oneToOneEntity.hashCode() : 0 );
		result = 31 * result + ( manyToOneEntity != null ? manyToOneEntity.hashCode() : 0 );
		result = 31 * result + ( internalComponent != null ? internalComponent.hashCode() : 0 );
		result = 31 * result + ( internalComponents != null ? internalComponents.hashCode() : 0 );
		return result;
	}
}
