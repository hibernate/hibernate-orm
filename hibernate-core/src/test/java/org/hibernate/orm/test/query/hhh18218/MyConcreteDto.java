package org.hibernate.orm.test.query.hhh18218;

class MyConcreteDto {
	protected Long id;

	protected String someOtherField;

	public MyConcreteDto(Long id, String someOtherField) {
		this.id = id;
		this.someOtherField = someOtherField;
	}
}
