package org.hibernate.test.version;

import java.io.Serializable;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;


@TestForIssue(jiraKey = "HHH-17380")
public class NewEntityWithNullVersionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "NewEntityWithNullVersion.hbm.xml" };
	}

	@Override
	protected String getBaseForMappings() {
		return super.getBaseForMappings() + "version/";
	}

	@Test
	public void testMergeDetachedEntityWithIdentityId() {
		UserAttributes item = new UserAttributes();
		item.id = 123L;
		item.name = "Abc";
		inTransaction(
				session -> {
					session.saveOrUpdate( item );
				}
		);
	}

	public static class UserAttributes implements Serializable {

		private Long id = 0L;

		private Long version;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}

