package org.hibernate.orm.test.annotations.override.mappedsuperclass;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.junit.jupiter.api.Test;

/**
 * @author Stanislav Gubanov
 */
@TestForIssue(jiraKey = "HHH-11771")
@DomainModel(
		annotatedClasses = {
				MappedSuperClassBasicPropertyIdAttributeOverrideTest.BaseMappedSuperClass.class,
				MappedSuperClassBasicPropertyIdAttributeOverrideTest.ExtendBase.class
		}
)
public class MappedSuperClassBasicPropertyIdAttributeOverrideTest {

	@Test
	public void test() {
	}

	@MappedSuperclass
	@Access(AccessType.FIELD)
	public static class BaseMappedSuperClass {

		@Id
		Long uid;

		public Long getUid() {
			return uid;
		}

		public void setUid(Long uid) {
			this.uid = uid;
		}
	}

	@Entity
	public static class ExtendBase extends BaseMappedSuperClass {

		@Access(AccessType.PROPERTY)
		@Override
		@AttributeOverride(name = "uid", column = @Column(name = "id_extend_table", insertable = false, updatable = false))
		public Long getUid() {
			return super.getUid();
		}
	}

}
