package org.hibernate.orm.test.schemaupdate.columnoptions;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.Table;

@Entity
@Table(name = "ANOTHER_TEST_ENTITY")
@PrimaryKeyJoinColumns(
		value = @PrimaryKeyJoinColumn(name = "joined_fk", options = ColumnOptionsTest.PRIMARY_KEY_JOIN_COLUMN_OPTIONS)
)
public class AnotherTestEntity extends TestEntity {

}
