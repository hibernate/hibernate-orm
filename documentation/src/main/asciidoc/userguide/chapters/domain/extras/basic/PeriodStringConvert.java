@Entity
public class Event  {

    @Id
    @GeneratedValue
    private Long id;

    @Convert(converter = PeriodStringConverter.class)
    private Period span;

    public Event() {}

    public Event(Period span) {
        this.span = span;
    }

    public Long getId() {
        return id;
    }

    public Period getSpan() {
        return span;
    }
}