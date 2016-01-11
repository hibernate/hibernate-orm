public enum Gender {

    MALE('M'),
    FEMALE('F');

    private final char code;

    private Gender( char code ) {
        this.code = code;
    }

    public static Gender fromCode( char code ) {
        if ( code == 'M' || code == 'm' ) {
            return MALE;
        }
        if ( code == 'F' || code == 'f' ) {
            return FEMALE;
        }
        throw...
    }

    public char getCode() {
        return code;
    }
}

@Entity
public class Person {
    ...

    @Basic
    @Convert( converter = GenderConverter.class )
    public Gender gender;
}

@Converter
public class GenderConverter implements AttributeConverter<Character, Gender> {

    public Character convertToDatabaseColumn( Gender value ) {
        if ( value == null ) {
            return null;
        }

        return value.getCode();
    }

    public Gender convertToEntityAttribute( Character value ) {
        if ( value == null ) {
            return null;
        }

        return Gender.fromCode( value );
    }
}