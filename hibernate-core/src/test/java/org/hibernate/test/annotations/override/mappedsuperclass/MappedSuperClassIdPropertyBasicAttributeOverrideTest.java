package org.hibernate.test.annotations.override.mappedsuperclass;

import org.hibernate.MappingException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Stanislav Gubanov
 */
@TestForIssue(jiraKey = "HHH-11771")
public class MappedSuperClassIdPropertyBasicAttributeOverrideTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				MappedSuperClassWithUuidAsBasic.class,
				SubclassWithUuidAsId.class
		};
	}

	@Override
	protected void buildResources() {
		try {
			super.buildResources();
			fail( "Should throw exception!" );
		}
		catch (MappingException expected) {
			assertEquals(
					"You cannot override the [uid] non-identifier property from the [org.hibernate.test.annotations.override.mappedsuperclass.MappedSuperClassWithUuidAsBasic] base class or @MappedSuperclass and make it an identifier in the [org.hibernate.test.annotations.override.mappedsuperclass.SubclassWithUuidAsId] subclass!",
					expected.getMessage()
			);
		}
	}

	@Test
	public void test() {
	}

}
