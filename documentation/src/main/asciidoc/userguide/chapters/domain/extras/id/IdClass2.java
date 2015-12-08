@Entity
@IdClass(PK.class)
public class Login {
	public static class PK implements Serializable {
		private System system;
		private String username;
		...
	}

	@Id
	@ManyToOne
	private System system;
	@Id
	private String username;

	...
}