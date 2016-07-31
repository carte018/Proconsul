package edu.duke.oit.idms.proconsul;

public class DisplayOu {
	// POJO to carry display OU values for selectors in JSP
	// 
	// name is what's displayed in the selector
	// value is what's sent to the handler
	private String name;
	private String value;
	DisplayOu(String name,String value) {
		if (name != null) {
			this.name = name;
		} else {
			this.name = value;
		}
		this.value = value;
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
