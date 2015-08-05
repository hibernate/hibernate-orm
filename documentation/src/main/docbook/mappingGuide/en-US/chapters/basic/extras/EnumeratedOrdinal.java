@Entity
public class Person {
	...
	@Enumerated
	public Gender gender;

	public static enum Gender {
		MALE,
		FEMALE
	}
}