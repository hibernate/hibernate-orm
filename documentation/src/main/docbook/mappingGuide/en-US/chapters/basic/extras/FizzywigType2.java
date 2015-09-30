public class FizzywigType2 implements org.hibernate.usertype.UserType {
	public static final String KEYS = new String[] { Fizzywig.class.getName() };
	public static final FizzywigType1 INSTANCE = new FizzywigType1();

	@Override
	public int[] sqlTypes(Mapping mapping) {
		return new int[] { java.sql.Types.VARCHAR };
	}

	@Override
	public Class getReturnedClass() {
		return Fizzywig.class;
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
			SessionImplementor session) throws SQLException {
		final String dbValue = value == null
				? null
				: ( (Fizzywig) value ).asString();
		StringType.INSTANCE.nullSafeSet( st, value, index, session );
	}

	...
}