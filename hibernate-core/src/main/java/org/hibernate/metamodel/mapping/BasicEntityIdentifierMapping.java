package org.hibernate.metamodel.mapping;

/**
 * Mapping for a simple, single-column identifier
 *
 * @author Steve Ebersole
 */
public interface BasicEntityIdentifierMapping<J, M, R> extends SingleAttributeIdentifierMapping<J, M, R>, BasicValuedModelPart<J, M, R> {
	@Override
	JdbcMapping<R> getJdbcMapping();

	@Override
	default int getFetchableKey() {
		return -1;
	}

	@Override
	boolean isNullable();

	@Override
	boolean isInsertable();

}
