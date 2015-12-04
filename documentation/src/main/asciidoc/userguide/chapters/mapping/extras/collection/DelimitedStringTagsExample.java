@Entity
public static class Post {
	@Id
	public Integer id;
	@Basic
	@Type( type = "delimited_strings" )
	Set<String> tags;
}


public static class DelimitedStringsType extends AbstractSingleColumnStandardBasicType<Set> {
	public DelimitedStringsType() {
		super(
				VarcharTypeDescriptor.INSTANCE,
				new DelimitedStringsJavaTypeDescriptor()
		);
	}

	@Override
	public String getName() {
		return "delimited_strings";
	}
}

public static class DelimitedStringsJavaTypeDescriptor extends AbstractTypeDescriptor<Set> {
	public DelimitedStringsJavaTypeDescriptor() {
		super(
				Set.class,
				new MutableMutabilityPlan<Set>() {
					@Override
					protected Set deepCopyNotNull(Set value) {
						Set<String> copy = new HashSet<String>();
						copy.addAll( value );
						return copy;
					}
				}
		);
	}

	@Override
	public String toString(Set value) {
		return null;
	}

	@Override
	public Set fromString(String string) {
		return null;
	}

	@Override
	public <X> X unwrap(Set value, Class<X> type, WrapperOptions options) {
		return null;
	}

	@Override
	public <X> Set wrap(X value, WrapperOptions options) {
		return null;
	}
}