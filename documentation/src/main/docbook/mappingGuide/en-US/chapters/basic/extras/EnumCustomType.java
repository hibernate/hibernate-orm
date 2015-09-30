import org.hibernate.type.descriptor.java.CharacterTypeDescriptor;

@Entity
public class Person {
	...
	@Basic
	@Type( type = GenderType.class )
	public Gender gender;
}

public enum Gender {
	MALE( 'M' ),
	FEMALE( 'F' );

	private final char code;

	private Gender(char code) {
		this.code = code;
	}

	public char getCode() {
		return code;
	}

	public static Gender fromCode(char code) {
		if ( code == 'M' || code == 'm' ) {
			return MALE;
		}
		if ( code == 'F' || code == 'f' ) {
			return FEMALE;
		}
		throw ...
	}
}

@Converter
public class GenderType
		extends AbstractSingleColumnStandardBasicType<Gender> {

	public static final GenderType INSTANCE = new GenderType();

	private GenderType() {
		super(
				CharTypeDescriptor.INSTANCE,
				GenderJavaTypeDescriptor.INSTANCE
		);
	}

	public String getName() {
		return "gender";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}

public static class GenderJavaTypeDescriptor
		extends AbstractTypeDescriptor<Gender> {
	public static final GenderJavaTypeDescriptor INSTANCE = new GenderJavaTypeDescriptor();

	public String toString(Gender value) {
		return value == null ? null : value.name();
	}

	public Gender fromString(String string) {
		return string == null ? null : Gender.valueOf( string );
	}

	public <X> X unwrap(Gender value, Class<X> type, WrapperOptions options) {
		return CharacterTypeDescriptor.INSTANCE.unwrap(
				value == null ? null : value.getCode(),
				type,
				options
		);
	}

	public <X> Gender wrap(X value, WrapperOptions options) {
		return CharacterTypeDescriptor.INSTANCE.wrap( value, options );
	}
}