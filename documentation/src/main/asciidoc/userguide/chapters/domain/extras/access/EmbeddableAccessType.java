@Embeddable
@Access(AccessType.PROPERTY)
public static class Change {

    private String path;

    private String diff;

    public Change() {}

    @Column(name = "path", nullable = false)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Column(name = "diff", nullable = false)
    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }
}