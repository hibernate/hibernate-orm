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
		return java.util.Objects.hash(ssn);
	}

	@Override
	public boolean equals(Object rhs) {
	/**
	 * A proxy class does not compare well. So we first have to get the real
	 * class
	 */
	if (rhs == null || Hibernate.getClass(this) != Hibernate.getClass(rhs))
		return false;
	Person that = (Person) rhs;

	if (!java.util.Objects.equals(ssn, that.ssn))
		return false;
	return true;
}