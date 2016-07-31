package edu.duke.oit.idms.proconsul.exceptions;

public class ContainerInitException extends RuntimeException {
	private static final long serialVersionUID = 919855401371104L;
	
	public ContainerInitException(String message, Throwable cause) {
		super(message, cause);
	}
	public ContainerInitException(String message) {
		this(message,null);
	}
	public ContainerInitException(Throwable cause) {
		this(cause.getMessage(),cause);
	}
}
