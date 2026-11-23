package org.hibernate.orm.test.type.descriptor.jdbc;

import java.sql.Types;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.type.descriptor.jdbc.JdbcTypeFamilyInformation;
import org.hibernate.type.descriptor.jdbc.JdbcTypeFamilyInformation.Family;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JdbcTypeFamilyInformationTest {
	@Test
	@JiraKey(value = "HHH-21175")
	public void everyTypeCodeOfAFamilyIsFound() {
		for ( Family family : Family.values() ) {
			for ( int typeCode : family.getTypeCodes() ) {
				assertThat( JdbcTypeFamilyInformation.INSTANCE.locateJdbcTypeFamilyByTypeCode( typeCode ) )
						.isEqualTo( family );
			}
		}
	}

	@Test
	@JiraKey(value = "HHH-21175")
	public void aTypeCodeOutsideEveryFamilyIsNotFound() {
		assertThat( JdbcTypeFamilyInformation.INSTANCE.locateJdbcTypeFamilyByTypeCode( Types.ARRAY ) ).isNull();
	}
}
