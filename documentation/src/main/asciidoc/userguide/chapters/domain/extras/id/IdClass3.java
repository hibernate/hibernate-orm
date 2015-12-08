@Entity
@IdClass(PK.class)
public class LogFile {
	public static class PK implements Serializable {
		private String name;
		private LocalDate date;
		private Integer uniqueStamp;
		...
	}

	@Id
	private String name;
	@Id
	private LocalDate date;
	@Id
	@GeneratedValue
	private Integer uniqueStamp;
	...
}

