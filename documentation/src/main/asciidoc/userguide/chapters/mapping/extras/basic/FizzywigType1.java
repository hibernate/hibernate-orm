public class FizzywigType1 implements org.hibernate.type.BasicType {
	public static final FizzywigType1 INSTANCE = new FizzywigType1();

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { Fizzywig.class.getName() };
	}

	@Override
	public int[] sqlTypes(Mapping mapping) {
		return new int[] { java.sql.Types.VARCHAR };
	}

	@Override
	public Class getReturnedClass() {
		return Money.class;
	}

	@Override
	public Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SessionImplementor session,
			Object owner) throws SQLException {
		return Fizzwig.fromString(
				StringType.INSTANCE.get( rs, names[0], sesson )
		);
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SessionImplementor session) throws SQLException {
		final String dbValue = value == null
				? null
				: ( (Fizzywig) value ).asString();
		StringType.INSTANCE.nullSafeSet( st, value, index, settable, session );
	}

	...
}