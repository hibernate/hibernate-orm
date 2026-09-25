package org.hibernate.boot.jaxb.hbm.transform;

/**
 * @author Steve Ebersole
 */
interface ColumnDefaults {
	Boolean isNullable();

	Integer getLength();

	Integer getScale();

	Integer getPrecision();

	Boolean isUnique();

	Boolean isInsertable();

	Boolean isUpdatable();
}
