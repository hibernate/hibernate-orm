@Entity
public class Person {
	@Id
	@GeneratedValue
	private Integer id;

	@NaturalId
	private String ssn;

	protected Person() {
		// ctor for ORM
	}

	public Person(String ssn) {
		// ctor for app
		this.ssn = ssn;
	}

	...

	@Override
	public int hashCode() {
		assert ssn != null;
		return ssn.hashCode();
	}

	@Override
	public boolean equals() {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof Person ) ) {
			return false;
		}

		final Person other = (Person) o;
		assert ssn != null;
		assert other.ssn != null;

		return ssn.equals( other.ssn );
	}
}