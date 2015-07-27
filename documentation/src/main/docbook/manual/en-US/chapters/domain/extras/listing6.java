@Entity
public class Person {
	@Id
	@GeneratedValue
	private Integer id;

	...

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}

	@Override
	public boolean equals() {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof Person ) ) {
			return false;
		}

		if ( id == null ) {
			return false;
		}

		final Person other = (Person) o;
		return id.equals( other.id );
	}
}