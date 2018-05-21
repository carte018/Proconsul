package edu.duke.oit.idms.proconsul;

public class DisplayGroup {
	private String name;
	private String value;
	public DisplayGroup(String value) {
		this.setValue(value);
		if (value != null) {
			this.setName(value.replaceAll("(?i)[^,]*,dc=.*","").substring(0, 25));
		} else {
			this.setName(null);
		}
	}
	public DisplayGroup(String name,String value) {
		this.setValue(value);
		this.setName(name);
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
}
