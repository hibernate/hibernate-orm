package org.hibernate.test.annotations.override.mappedsuperclass;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Stanislav Gubanov
 */
@TestForIssue(jiraKey = "HHH-11771")
public class MappedSuperClassBasicPropertyIdAttributeOverrideTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void test() {
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				BaseMappedSuperClass.class,
				ExtendBase.class
		};
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
