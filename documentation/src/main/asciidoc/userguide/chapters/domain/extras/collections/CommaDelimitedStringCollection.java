@Entity
public class Person  {

    @Id
    private Long id;

    @Type(type = "comma_delimited_strings")
    private List<String> phones = new ArrayList<>();

    public List<String> getPhones() {
        return phones;
    }
}

public class CommaDelimitedStringsJavaTypeDescriptor extends AbstractTypeDescriptor<List> {

    public static final String DELIMITER = ",";

    public CommaDelimitedStringsJavaTypeDescriptor() {
        super(
            List.class,
            new MutableMutabilityPlan<List>() {
                @Override
                protected List deepCopyNotNull(List value) {
                    return new ArrayList(value);
                }
            }
        );
    }

    @Override
    public String toString(List value) {
        return ((List<String>) value).stream().collect(Collectors.joining(DELIMITER));
    }

    @Override
    public List fromString(String string) {
        List<String> values = new ArrayList<>();
        Collections.addAll(values, string.split(DELIMITER));
        return values;
    }

    @Override
    public <X> X unwrap(List value, Class<X> type, WrapperOptions options) {
        return (X) toString(value);
    }

    @Override
    public <X> List wrap(X value, WrapperOptions options) {
        return fromString((String) value);
    }
}

public class CommaDelimitedStringsType extends AbstractSingleColumnStandardBasicType<List> {

    public CommaDelimitedStringsType() {
        super(
            VarcharTypeDescriptor.INSTANCE,
            new CommaDelimitedStringsJavaTypeDescriptor()
        );
    }

    @Override
    public String getName() {
        return "comma_delimited_strings";
    }
}
