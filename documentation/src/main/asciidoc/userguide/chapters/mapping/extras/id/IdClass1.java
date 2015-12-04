@Entity
@IdClass(PK.class)
public class Login {
	public static class PK implements Serializable  {
		private String system;
		private String username;
		...
	}

	@Id
	private String system;
	@Id
	private String username;
	...
}